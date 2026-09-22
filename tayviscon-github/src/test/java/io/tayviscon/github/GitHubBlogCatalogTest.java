package io.tayviscon.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.tayviscon.core.blog.BlogUnavailableException;
import io.tayviscon.renderer.MarkdownRenderer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GitHubBlogCatalogTest {
  private static final String SYSTEMS_INDEX =
      """
      ---
      title: Надежные системы
      summary: Модульная архитектура
      date: 2026-09-20
      draft: false
      ---
      См. ![движок](./engine.excalidraw.svg)
      """;

  private static final String DRAFT_INDEX =
      """
      ---
      title: Скрыто
      summary: нет
      date: 2026-09-21
      draft: true
      ---
      нет
      """;

  private static final String AGENTS_INDEX =
      """
      ---
      title: Агенты
      summary: Вторая
      date: 2026-09-18
      ---
      текст
      """;

  @Test
  void treeOmitsDrafts() {
    var catalog = catalog(fixture());
    var tree = catalog.tree();
    assertEquals(2, tree.size());
    assertEquals("systems", tree.get(0).name());
    assertEquals(1, tree.get(0).articles().size());
    assertEquals("Надежные системы", tree.get(0).articles().get(0).title());
    assertEquals("systems/online-code-execution", tree.get(0).articles().get(0).path());
    assertEquals("ai", tree.get(1).name());
    assertEquals(1, tree.get(1).articles().size());
    assertEquals("ai/agents", tree.get(1).articles().get(0).path());
    assertFalse(
        tree.stream()
            .flatMap(group -> group.articles().stream())
            .anyMatch(item -> item.path().contains("drafty")));
  }

  @Test
  void latestReturnsTwoNewestByDate() {
    var latest = catalog(fixture()).latest();
    assertEquals(2, latest.size());
    assertEquals("2026-09-20", latest.get(0).date());
    assertEquals("systems/online-code-execution", latest.get(0).path());
    assertEquals("2026-09-18", latest.get(1).date());
    assertEquals("ai/agents", latest.get(1).path());
  }

  @Test
  void articleRewritesImageAndIsFragment() {
    var article = catalog(fixture()).article("systems/online-code-execution");
    assertTrue(article.isPresent());
    assertTrue(
        article
            .get()
            .html()
            .contains(
                "/api/blog/assets/systems/online-code-execution/engine.excalidraw.svg"));
    assertFalse(article.get().html().contains("<html>"));
  }

  @Test
  void articleUnknownOrDraftIsEmpty() {
    var catalog = catalog(fixture());
    assertTrue(catalog.article("systems/drafty").isEmpty());
    assertTrue(catalog.article("nope").isEmpty());
  }

  @Test
  void assetPublishedOkDraftHidden() {
    var catalog = catalog(fixture());
    var svg = catalog.asset("systems/online-code-execution/engine.excalidraw.svg");
    assertTrue(svg.isPresent());
    assertEquals("image/svg+xml", svg.get().contentType());
    assertEquals("<svg/>", new String(svg.get().bytes(), StandardCharsets.UTF_8));
    assertTrue(catalog.asset("systems/drafty/x.png").isEmpty());
  }

  @Test
  void usesStaleSnapshotWhenGithubFails() {
    var client = fixture();
    var catalog = catalog(client);
    assertEquals(2, catalog.tree().size());
    assertTrue(catalog.asset("systems/online-code-execution/engine.excalidraw.svg").isPresent());
    client.fail = true;
    catalog.evict();
    assertEquals("systems", catalog.tree().getFirst().name());
    assertTrue(catalog.asset("systems/online-code-execution/engine.excalidraw.svg").isPresent());
  }

  @Test
  void throwsWhenNoSnapshot() {
    var client = new FakeContents();
    client.fail = true;
    var catalog = catalog(client);
    assertThrows(BlogUnavailableException.class, catalog::tree);
  }

  @Test
  void skipsMalformedFrontMatterAndKeepsPublishedArticle() {
    var client = new FakeContents();
    client.put("systems/online-code-execution/index.md", SYSTEMS_INDEX);
    client.put(
        "broken/bad/index.md",
        """
        ---
        title: [unclosed
        date: 2026-09-19
        ---
        сломанный yaml
        """);
    var catalog = catalog(client);

    var tree = catalog.tree();
    assertEquals(1, tree.size());
    assertEquals("systems", tree.getFirst().name());
    assertEquals(1, tree.getFirst().articles().size());
    assertEquals("systems/online-code-execution", tree.getFirst().articles().getFirst().path());

    var latest = catalog.latest();
    assertEquals(1, latest.size());
    assertEquals("systems/online-code-execution", latest.getFirst().path());
  }

  private static GitHubBlogCatalog catalog(FakeContents client) {
    return new GitHubBlogCatalog(client, new MarkdownRenderer());
  }

  private static FakeContents fixture() {
    var client = new FakeContents();
    client.put("systems/online-code-execution/index.md", SYSTEMS_INDEX);
    client.put("systems/online-code-execution/engine.excalidraw.svg", "<svg/>");
    client.put("systems/drafty/index.md", DRAFT_INDEX);
    client.put("ai/agents/index.md", AGENTS_INDEX);
    return client;
  }

  static final class FakeContents implements GitHubContentsClient {
    boolean fail = false;
    private final Map<String, String> files = new LinkedHashMap<>();

    void put(String path, String content) {
      files.put(path, content);
    }

    @Override
    public String getFile(String path) {
      if (fail) {
        throw new RuntimeException("GitHub unavailable");
      }
      var content = files.get(path);
      if (content == null) {
        throw new RuntimeException("Not found: " + path);
      }
      return content;
    }

    @Override
    public byte[] getRaw(String path) {
      return getFile(path).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public List<GitHubTreeEntry> listTree() {
      if (fail) {
        throw new RuntimeException("GitHub unavailable");
      }
      Set<String> dirs = new LinkedHashSet<>();
      List<GitHubTreeEntry> entries = new ArrayList<>();
      for (String path : files.keySet()) {
        int slash = path.indexOf('/');
        while (slash >= 0) {
          String dir = path.substring(0, slash);
          if (dirs.add(dir)) {
            entries.add(new GitHubTreeEntry(dir, true));
          }
          slash = path.indexOf('/', slash + 1);
        }
        entries.add(new GitHubTreeEntry(path, false));
      }
      return entries;
    }
  }
}
