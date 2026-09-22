package io.tayviscon.github;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.tayviscon.core.blog.BlogArticle;
import io.tayviscon.core.blog.BlogAsset;
import io.tayviscon.core.blog.BlogCatalog;
import io.tayviscon.core.blog.BlogPost;
import io.tayviscon.core.blog.BlogTreeGroup;
import io.tayviscon.core.blog.BlogTreeItem;
import io.tayviscon.core.blog.BlogUnavailableException;
import io.tayviscon.renderer.MarkdownRenderer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Каталог блога из tayviscon-knowledge-base: кэш на неделю и last-good снимок. */
public class GitHubBlogCatalog implements BlogCatalog {
  private static final Logger log = LoggerFactory.getLogger(GitHubBlogCatalog.class);
  private static final String CACHE_KEY = "blog";
  private static final String DEFAULT_SORT_DATE = "0001-01-01";
  private static final Pattern FRONT_MATTER =
      Pattern.compile("(?s)^---\\r?\\n(.*?)\\r?\\n---\\r?\\n?(.*)$");

  private final GitHubContentsClient client;
  private final MarkdownRenderer renderer;
  private final ObjectMapper yamlMapper =
      new ObjectMapper(new YAMLFactory())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final LoadingCache<String, Snapshot> cache;
  private volatile Snapshot lastGood;

  /** Создаёт каталог с недельным кэшем поверх клиента GitHub и Markdown-рендерера. */
  public GitHubBlogCatalog(GitHubContentsClient client, MarkdownRenderer renderer) {
    this.client = client;
    this.renderer = renderer;
    this.cache =
        Caffeine.newBuilder()
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build(key -> loadSnapshot());
  }

  @Override
  public List<BlogTreeGroup> tree() {
    return getSnapshot().tree();
  }

  @Override
  public List<BlogPost> latest() {
    return getSnapshot().latest();
  }

  @Override
  public Optional<BlogArticle> article(String path) {
    if (unsafe(path)) {
      return Optional.empty();
    }
    return Optional.ofNullable(getSnapshot().articles().get(path));
  }

  @Override
  public Optional<BlogAsset> asset(String path) {
    if (unsafe(path)) {
      return Optional.empty();
    }
    return Optional.ofNullable(getSnapshot().assets().get(path));
  }

  void evict() {
    cache.invalidate(CACHE_KEY);
  }

  private Snapshot getSnapshot() {
    try {
      return cache.get(CACHE_KEY);
    } catch (RuntimeException e) {
      if (lastGood != null) {
        log.warn("GitHub blog fetch failed; serving last-good snapshot", e);
        return lastGood;
      }
      if (e instanceof BlogUnavailableException blogUnavailable) {
        throw blogUnavailable;
      }
      throw new BlogUnavailableException("Blog unavailable", e);
    }
  }

  private Snapshot loadSnapshot() {
    try {
      var snapshot = fetchSnapshot();
      lastGood = snapshot;
      return snapshot;
    } catch (RuntimeException e) {
      throw new BlogUnavailableException("Blog unavailable", e);
    }
  }

  private Snapshot fetchSnapshot() {
    List<GitHubTreeEntry> tree = client.listTree();
    Map<String, BlogArticle> articles = new LinkedHashMap<>();
    Map<String, BlogAsset> assets = new LinkedHashMap<>();
    for (GitHubTreeEntry entry : tree) {
      if (entry.directory() || !entry.path().endsWith("/index.md")) {
        continue;
      }
      String articlePath =
          entry.path().substring(0, entry.path().length() - "/index.md".length());
      try {
        Loaded loaded = parseMarkdown(client.getFile(entry.path()));
        if (!loaded.published()) {
          continue;
        }
        String html = renderer.toFragment(loaded.body(), articlePath);
        articles.put(
            articlePath,
            new BlogArticle(
                loaded.title(), loaded.summary(), loaded.date(), articlePath, html));
        loadAssets(tree, articlePath, assets);
      } catch (RuntimeException e) {
        log.warn("Skipping blog article due to parse or fetch failure: {}", entry.path(), e);
      }
    }
    return new Snapshot(
        buildTree(articles), buildLatest(articles), Map.copyOf(articles), Map.copyOf(assets));
  }

  private void loadAssets(
      List<GitHubTreeEntry> tree, String articlePath, Map<String, BlogAsset> assets) {
    String prefix = articlePath + "/";
    for (GitHubTreeEntry blob : tree) {
      if (blob.directory()) {
        continue;
      }
      String blobPath = blob.path();
      if (!blobPath.startsWith(prefix) || blobPath.endsWith("/index.md")) {
        continue;
      }
      assets.put(blobPath, new BlogAsset(client.getRaw(blobPath), contentType(blobPath)));
    }
  }

  private Loaded parseMarkdown(String markdown) {
    Matcher matcher = FRONT_MATTER.matcher(markdown == null ? "" : markdown);
    if (!matcher.matches()) {
      return Loaded.unpublished();
    }
    try {
      FrontMatter frontMatter = yamlMapper.readValue(matcher.group(1), FrontMatter.class);
      return Loaded.from(frontMatter, matcher.group(2));
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse blog front matter", e);
    }
  }

  private static List<BlogTreeGroup> buildTree(Map<String, BlogArticle> articles) {
    Map<String, List<BlogTreeItem>> groups = new LinkedHashMap<>();
    for (BlogArticle article : articles.values()) {
      groups
          .computeIfAbsent(groupName(article.path()), key -> new ArrayList<>())
          .add(new BlogTreeItem(article.title(), article.path()));
    }
    List<BlogTreeGroup> result = new ArrayList<>();
    for (var entry : groups.entrySet()) {
      result.add(new BlogTreeGroup(entry.getKey(), List.copyOf(entry.getValue())));
    }
    return List.copyOf(result);
  }

  private static String groupName(String articlePath) {
    int slash = articlePath.indexOf('/');
    return slash < 0 ? articlePath : articlePath.substring(0, slash);
  }

  private static List<BlogPost> buildLatest(Map<String, BlogArticle> articles) {
    return articles.values().stream()
        .sorted(
            Comparator.comparing(GitHubBlogCatalog::sortDate)
                .reversed()
                .thenComparing(BlogArticle::path))
        .limit(2)
        .map(
            article ->
                new BlogPost(
                    article.title(), article.summary(), article.date(), article.path()))
        .toList();
  }

  private static String sortDate(BlogArticle article) {
    String date = article.date();
    return date == null || date.isBlank() ? DEFAULT_SORT_DATE : date;
  }

  private static boolean unsafe(String path) {
    return path == null || path.contains("..");
  }

  private static String contentType(String path) {
    String lower = path.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".svg")) {
      return "image/svg+xml";
    }
    if (lower.endsWith(".png")) {
      return "image/png";
    }
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    return "application/octet-stream";
  }

  private record FrontMatter(String title, String summary, String date, Boolean draft) {}

  private record Loaded(
      String title, String summary, String date, String body, boolean published) {
    static Loaded unpublished() {
      return new Loaded("", "", "", "", false);
    }

    static Loaded from(FrontMatter frontMatter, String body) {
      boolean published =
          frontMatter != null
              && frontMatter.title() != null
              && !frontMatter.title().isBlank()
              && !Boolean.TRUE.equals(frontMatter.draft());
      return new Loaded(
          frontMatter == null ? "" : nullToEmpty(frontMatter.title()),
          frontMatter == null ? "" : nullToEmpty(frontMatter.summary()),
          frontMatter == null ? "" : nullToEmpty(frontMatter.date()),
          body == null ? "" : body,
          published);
    }

    private static String nullToEmpty(String value) {
      return value == null ? "" : value;
    }
  }

  private record Snapshot(
      List<BlogTreeGroup> tree,
      List<BlogPost> latest,
      Map<String, BlogArticle> articles,
      Map<String, BlogAsset> assets) {}
}
