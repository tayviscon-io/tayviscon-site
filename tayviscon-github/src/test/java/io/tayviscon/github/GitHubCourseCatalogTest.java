package io.tayviscon.github;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.tayviscon.core.course.CatalogUnavailableException;
import io.tayviscon.core.course.OutlineNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GitHubCourseCatalogTest {
  private static final String catalogYaml =
      """
      series: yet-another-course
      courses:
        - id: sql
          title: Yet Another SQL Course
          path: courses/sql
          status: published
      """;

  private static final String sqlInfoYaml =
      """
      type: marketplace
      title: Yet Another SQL Course
      summary: SQL course summary
      content:
        - Введение
      yaml_version: 5
      """;

  private static final String javaCatalogYaml =
      """
      series: yet-another-course
      courses:
        - id: java
          title: Yet Another Java Course
          path: courses/java
          status: stub
      """;

  private static final String javaInfoYaml =
      """
      type: marketplace
      title: Yet Another Java Course
      summary: Java course summary
      content:
        - Section 1
      additional_files:
        - name: build.gradle
      yaml_version: 5
      """;

  @Test
  void usesStaleSnapshotWhenGithubFails() {
    var client = new FakeContents();
    client.put("catalog.yaml", catalogYaml);
    client.put("courses/sql/course-info.yaml", sqlInfoYaml);
    var catalog = new GitHubCourseCatalog(client, new YamlCatalogParser(), new SvgLogoCleaner());
    assertEquals(1, catalog.list().size());
    final int afterSuccess = client.getFileCount;
    catalog.evict();
    client.fail = true;
    assertEquals("sql", catalog.list().getFirst().id());
    assertTrue(client.getFileCount > afterSuccess);
    int afterFailedReload = client.getFileCount;
    assertEquals("sql", catalog.list().getFirst().id());
    assertTrue(client.getFileCount > afterFailedReload);
  }

  @Test
  void throwsWhenNoSnapshot() {
    var client = new FakeContents();
    client.fail = true;
    var catalog = new GitHubCourseCatalog(client, new YamlCatalogParser(), new SvgLogoCleaner());
    assertThrows(CatalogUnavailableException.class, catalog::list);
  }

  @Test
  void missingLogoSetsFallbackLetter() {
    var client = new FakeContents();
    client.put("catalog.yaml", javaCatalogYaml);
    client.put("courses/java/course-info.yaml", javaInfoYaml);
    var catalog = new GitHubCourseCatalog(client, new YamlCatalogParser(), new SvgLogoCleaner());
    assertEquals("J", catalog.list().getFirst().fallbackLetter());
    assertFalse(catalog.list().getFirst().hasLogo());
  }

  @Test
  void missingLogoBytesSetsFallbackLetter() {
    var client = new FakeContents();
    client.put("catalog.yaml", catalogYaml);
    client.put(
        "courses/sql/course-info.yaml",
        """
        type: marketplace
        title: Yet Another SQL Course
        summary: SQL course summary
        content:
          - Введение
        additional_files:
          - name: common/image/yet-another-sql-course-logo.svg
        yaml_version: 5
        """);
    var catalog = new GitHubCourseCatalog(client, new YamlCatalogParser(), new SvgLogoCleaner());
    assertEquals(1, catalog.list().size());
    assertEquals("S", catalog.list().getFirst().fallbackLetter());
    assertFalse(catalog.list().getFirst().hasLogo());
  }

  @Test
  void servesPngLogoOnCourseDetail() {
    var png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47};
    var client = new FakeContents();
    client.put("catalog.yaml", catalogYaml);
    client.put(
        "courses/sql/course-info.yaml",
        """
        type: marketplace
        title: Yet Another SQL Course
        summary: SQL course summary
        content:
          - Введение
        additional_files:
          - name: common/image/yet-another-sql-course-logo.png
        yaml_version: 5
        """);
    client.putRaw("courses/sql/common/image/yet-another-sql-course-logo.png", png);
    var catalog = new GitHubCourseCatalog(client, new YamlCatalogParser(), new SvgLogoCleaner());
    var listed = catalog.list().getFirst();
    assertTrue(listed.hasLogo());
    var logo = catalog.findById("sql").orElseThrow().logo();
    assertArrayEquals(png, logo.bytes());
    assertEquals("image/png", logo.contentType());
  }

  @Test
  void expandsSectionLessonsAndTasksInFindById() {
    var client = nestedSqlContents();
    var catalog = new GitHubCourseCatalog(client, new YamlCatalogParser(), new SvgLogoCleaner());
    var outline = catalog.findById("sql").orElseThrow().outline();
    assertEquals("Введение", outline.get(0).title());
    assertTrue(outline.get(0).children().isEmpty());
    var section = outline.get(1);
    assertEquals("Основы SQL и баз данных", section.title());
    assertEquals("Онбординг", section.children().get(0).title());
    assertTrue(section.children().get(0).children().isEmpty());
    var lesson = section.children().get(1);
    assertEquals("Транзакции и блокировки", lesson.title());
    assertEquals(
        List.of("Транзакции", "Блокировки"),
        lesson.children().stream().map(OutlineNode::title).toList());
  }

  @Test
  void skipsBrokenCourseAndKeepsHealthyOne() {
    var twoCourses =
        """
        series: yet-another-course
        courses:
          - id: sql
            title: Yet Another SQL Course
            path: courses/sql
            status: published
          - id: java
            title: Yet Another Java Course
            path: courses/java
            status: stub
        """;
    var client = new FakeContents();
    client.put("catalog.yaml", twoCourses);
    client.put("courses/sql/course-info.yaml", sqlInfoYaml);
    var catalog = new GitHubCourseCatalog(client, new YamlCatalogParser(), new SvgLogoCleaner());
    var listed = catalog.list();
    assertEquals(1, listed.size());
    assertEquals("sql", listed.getFirst().id());
  }

  @Test
  void cyclicOutlineFallsBackToSnapshot() {
    var client = new FakeContents();
    client.put("catalog.yaml", catalogYaml);
    client.put(
        "courses/sql/course-info.yaml",
        """
        title: Yet Another SQL Course
        summary: SQL course summary
        content:
          - Loop
        """);
    client.put(
        "courses/sql/Loop/section-info.yaml",
        """
        content:
          - Loop
        """);
    var catalog = new GitHubCourseCatalog(client, new YamlCatalogParser(), new SvgLogoCleaner());
    var detail = catalog.findById("sql").orElseThrow();
    assertEquals(List.of("Loop"), detail.sections());
    assertTrue(detail.outline().getFirst().children().isEmpty());
  }

  @Test
  void secondFindByIdDoesNotReloadOutline() {
    var client = nestedSqlContents();
    var catalog = new GitHubCourseCatalog(client, new YamlCatalogParser(), new SvgLogoCleaner());
    catalog.findById("sql").orElseThrow();
    int afterFirst = client.getFileCount;
    assertTrue(afterFirst > 2);
    catalog.findById("sql").orElseThrow();
    assertEquals(afterFirst, client.getFileCount);
  }

  @Test
  void logoBytesComeFromSnapshotWithoutWalkingOutline() {
    var png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47};
    var client = nestedSqlContents();
    client.put(
        "courses/sql/course-info.yaml",
        """
        title: Yet Another SQL Course
        summary: SQL course summary
        content:
          - Введение
          - Основы SQL и баз данных
        additional_files:
          - name: common/image/yet-another-sql-course-logo.png
        """);
    client.putRaw("courses/sql/common/image/yet-another-sql-course-logo.png", png);
    var catalog = new GitHubCourseCatalog(client, new YamlCatalogParser(), new SvgLogoCleaner());
    catalog.list();
    int afterList = client.getFileCount;
    int afterRaw = client.getRawCount;
    var logo = catalog.logo("sql").orElseThrow();
    assertArrayEquals(png, logo.bytes());
    assertEquals(afterList, client.getFileCount);
    assertEquals(afterRaw, client.getRawCount);
  }

  private static FakeContents nestedSqlContents() {
    var client = new FakeContents();
    client.put("catalog.yaml", catalogYaml);
    client.put(
        "courses/sql/course-info.yaml",
        """
        title: Yet Another SQL Course
        summary: SQL course summary
        content:
          - Введение
          - Основы SQL и баз данных
        """);
    client.put("courses/sql/Введение/lesson-info.yaml", "{}\n");
    client.put(
        "courses/sql/Основы SQL и баз данных/section-info.yaml",
        """
        content:
          - Онбординг
          - Транзакции и блокировки
        """);
    client.put("courses/sql/Основы SQL и баз данных/Онбординг/lesson-info.yaml", "{}\n");
    client.put(
        "courses/sql/Основы SQL и баз данных/Транзакции и блокировки/lesson-info.yaml",
        """
        content:
          - Транзакции
          - Блокировки
        """);
    return client;
  }

  static final class FakeContents implements GitHubContentsClient {
    boolean fail = false;
    int getFileCount = 0;
    int getRawCount = 0;
    private final Map<String, String> files = new HashMap<>();
    private final Map<String, byte[]> rawFiles = new HashMap<>();

    void put(String path, String content) {
      files.put(path, content);
    }

    void putRaw(String path, byte[] content) {
      rawFiles.put(path, content);
    }

    @Override
    public String getFile(String path) {
      getFileCount++;
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
      getRawCount++;
      if (fail) {
        throw new RuntimeException("GitHub unavailable");
      }
      var content = rawFiles.get(path);
      if (content == null) {
        throw new RuntimeException("Not found: " + path);
      }
      return content;
    }
  }
}
