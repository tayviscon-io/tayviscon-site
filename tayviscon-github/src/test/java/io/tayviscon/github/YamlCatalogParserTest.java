package io.tayviscon.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.tayviscon.core.course.CourseStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class YamlCatalogParserTest {
  @Test
  void parsesLegacyStubAndSections() throws IOException {
    var parser = new YamlCatalogParser();
    var catalog = parser.parseCatalog(load("catalog-sample.yaml"));
    assertEquals("java", catalog.get(0).id());
    assertEquals(CourseStatus.IN_PROGRESS, CourseStatus.fromCatalog(catalog.get(0).status()));
    var info = parser.parseCourseInfo(load("course-info-sample.yaml"));
    assertEquals("Yet Another SQL Course", info.title());
    assertFalse(info.summary().isBlank());
    assertEquals(
        List.of(
            "Введение",
            "Подготовка к обучению",
            "Основы SQL и баз данных",
            "Продвинутый SQL для работы с данными",
            "Серверное программирование на SQL"),
        info.sections());
    assertEquals("common/image/yet-another-sql-course-logo.svg", info.logoPath().orElseThrow());
  }

  @Test
  void prefersPngLogoWhenNoSvgIsListed() {
    var parser = new YamlCatalogParser();
    var info =
        parser.parseCourseInfo(
            """
            title: Yet Another Java Course
            summary: Java
            content:
              - Введение
            additional_files:
              - name: build.gradle
              - name: common/image/yet-another-java-course-logo.png
            """);
    assertEquals(
        "common/image/yet-another-java-course-logo.png", info.logoPath().orElseThrow());
  }

  private static String load(String resource) throws IOException {
    try (var stream = YamlCatalogParserTest.class.getClassLoader().getResourceAsStream(resource)) {
      assertNotNull(stream, resource);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
