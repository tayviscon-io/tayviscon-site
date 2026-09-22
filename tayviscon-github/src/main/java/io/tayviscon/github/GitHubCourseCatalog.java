package io.tayviscon.github;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.tayviscon.core.course.CatalogUnavailableException;
import io.tayviscon.core.course.Course;
import io.tayviscon.core.course.CourseCatalog;
import io.tayviscon.core.course.CourseDetail;
import io.tayviscon.core.course.CourseStatus;
import io.tayviscon.core.course.LogoAsset;
import io.tayviscon.core.course.OutlineNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link io.tayviscon.core.course.CourseCatalog} на основе GitHub Contents с кэшем
 * последнего удачного снимка.
 */
public class GitHubCourseCatalog implements CourseCatalog {
  private static final Logger log = LoggerFactory.getLogger(GitHubCourseCatalog.class);
  private static final String CACHE_KEY = "catalog";
  private static final int MAX_OUTLINE_DEPTH = 8;

  private final GitHubContentsClient client;
  private final YamlCatalogParser parser;
  private final SvgLogoCleaner logoCleaner;
  private final LoadingCache<String, List<CourseDetail>> cache;
  private final ConcurrentHashMap<String, CourseDetail> expandedOutlines =
      new ConcurrentHashMap<>();
  private volatile List<CourseDetail> lastGood;

  /** Создаёт каталог, который обновляется с GitHub раз в неделю. */
  public GitHubCourseCatalog(
      GitHubContentsClient client, YamlCatalogParser parser, SvgLogoCleaner logoCleaner) {
    this.client = client;
    this.parser = parser;
    this.logoCleaner = logoCleaner;
    this.cache =
        Caffeine.newBuilder()
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build(key -> loadSnapshot());
  }

  @Override
  public List<Course> list() {
    return getSnapshot().stream().map(CourseDetail::course).toList();
  }

  @Override
  public Optional<CourseDetail> findById(String id) {
    return snapshotDetail(id).map(this::expandedDetail);
  }

  @Override
  public Optional<LogoAsset> logo(String id) {
    return snapshotDetail(id).map(CourseDetail::logo);
  }

  void evict() {
    cache.invalidate(CACHE_KEY);
  }

  private List<CourseDetail> getSnapshot() {
    try {
      return cache.get(CACHE_KEY);
    } catch (RuntimeException e) {
      if (lastGood != null) {
        log.warn("GitHub catalog fetch failed; serving last-good snapshot", e);
        return lastGood;
      }
      if (e instanceof CatalogUnavailableException catalogUnavailable) {
        throw catalogUnavailable;
      }
      throw new CatalogUnavailableException("Catalog unavailable", e);
    }
  }

  private List<CourseDetail> loadSnapshot() {
    try {
      var snapshot = fetchCatalog();
      lastGood = snapshot;
      expandedOutlines.clear();
      return snapshot;
    } catch (RuntimeException e) {
      throw new CatalogUnavailableException("Catalog unavailable", e);
    }
  }

  private Optional<CourseDetail> snapshotDetail(String id) {
    return getSnapshot().stream().filter(detail -> detail.course().id().equals(id)).findFirst();
  }

  private CourseDetail expandedDetail(CourseDetail detail) {
    try {
      return expandedOutlines.computeIfAbsent(
          detail.course().id(), key -> withNestedOutline(detail));
    } catch (RuntimeException e) {
      log.warn(
          "GitHub outline expand failed; serving snapshot outline for {}",
          detail.course().id(),
          e);
      return detail;
    }
  }

  private List<CourseDetail> fetchCatalog() {
    var catalogYaml = client.getFile("catalog.yaml");
    var entries = parser.parseCatalog(catalogYaml);
    List<CourseDetail> details = new ArrayList<>();
    for (var entry : entries) {
      try {
        details.add(loadCourseDetail(entry));
      } catch (RuntimeException e) {
        log.warn("Skipping catalog entry {}", entry.id(), e);
      }
    }
    return details;
  }

  private CourseDetail loadCourseDetail(YamlCatalogParser.CatalogEntry entry) {
    var infoPath = "courses/" + entry.id() + "/course-info.yaml";
    var infoYaml = client.getFile(infoPath);
    var info = parser.parseCourseInfo(infoYaml);
    var logo = loadLogo(entry.id(), info.logoPath());
    var fallbackLetter = String.valueOf(Character.toUpperCase(entry.id().charAt(0)));
    var course =
        new Course(
            entry.id(),
            info.title(),
            info.summary(),
            CourseStatus.fromCatalog(entry.status()),
            fallbackLetter,
            logo != null);
    var idePath = "courses/" + entry.id() + "/";
    return new CourseDetail(course, OutlineNode.leaves(info.sections()), idePath, logo);
  }

  private CourseDetail withNestedOutline(CourseDetail detail) {
    var parent = "courses/" + detail.course().id();
    var outline =
        detail.sections().stream()
            .map(title -> loadNode(parent, title, new HashSet<>(), 0))
            .toList();
    return new CourseDetail(detail.course(), outline, detail.idePath(), detail.logo());
  }

  private OutlineNode loadNode(String parent, String title, Set<String> visited, int depth) {
    if (depth > MAX_OUTLINE_DEPTH || !visited.add(title)) {
      throw new IllegalStateException("outline cycle or too deep");
    }
    var folder = parent + "/" + title;
    try {
      var sectionYaml = client.getFileIfPresent(folder + "/section-info.yaml");
      if (sectionYaml.isPresent()) {
        var names = parser.parseContentList(sectionYaml.get());
        return new OutlineNode(
            title,
            names.stream().map(child -> loadNode(folder, child, visited, depth + 1)).toList());
      }
      var lessonYaml = client.getFileIfPresent(folder + "/lesson-info.yaml");
      if (lessonYaml.isPresent()) {
        return new OutlineNode(
            title, OutlineNode.leaves(parser.parseContentList(lessonYaml.get())));
      }
      return OutlineNode.leaf(title);
    } finally {
      visited.remove(title);
    }
  }

  private LogoAsset loadLogo(String id, Optional<String> logoPath) {
    if (logoPath.isEmpty()) {
      return null;
    }
    try {
      var relative = logoPath.get();
      var path = "courses/" + id + "/" + relative;
      var raw = client.getRaw(path);
      if (relative.toLowerCase(Locale.ROOT).endsWith(".png")) {
        return new LogoAsset(raw, "image/png");
      }
      var svg = logoCleaner.clean(new String(raw, StandardCharsets.UTF_8));
      if (svg == null) {
        return null;
      }
      return new LogoAsset(svg.getBytes(StandardCharsets.UTF_8), "image/svg+xml");
    } catch (RuntimeException e) {
      return null;
    }
  }
}
