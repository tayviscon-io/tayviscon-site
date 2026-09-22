package io.tayviscon.github;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Разбирает YAML каталога, course-info и содержимого из репозитория курсов. */
public class YamlCatalogParser {
  private final ObjectMapper mapper =
      new ObjectMapper(new YAMLFactory())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  /** Строка курса из {@code catalog.yaml}. */
  public record CatalogEntry(String id, String title, String path, String status) {}

  /** Разобранный {@code course-info.yaml} с необязательным путём к логотипу. */
  public record CourseInfo(
      String title, String summary, List<String> sections, Optional<String> logoPath) {}

  /** Разбирает {@code catalog.yaml} в список курсов. */
  public List<CatalogEntry> parseCatalog(String yaml) {
    try {
      var document = mapper.readValue(yaml, CatalogDocument.class);
      return document.courses().stream()
          .map(entry -> new CatalogEntry(entry.id(), entry.title(), entry.path(), entry.status()))
          .toList();
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse catalog YAML", e);
    }
  }

  /** Разбирает {@code course-info.yaml}: название, описание, разделы и логотип. */
  public CourseInfo parseCourseInfo(String yaml) {
    try {
      var document = mapper.readValue(yaml, CourseInfoDocument.class);
      var logoPath =
          document.additionalFiles() == null
              ? Optional.<String>empty()
              : document.additionalFiles().stream()
                  .map(CourseInfoDocument.AdditionalFile::name)
                  .filter(YamlCatalogParser::isLogoFile)
                  .findFirst();
      return new CourseInfo(
          document.title(),
          document.summary(),
          document.content() == null ? List.of() : document.content(),
          logoPath);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse course info YAML", e);
    }
  }

  /** Разбирает список {@code content} из YAML раздела или урока. */
  public List<String> parseContentList(String yaml) {
    try {
      var document = mapper.readValue(yaml, CourseInfoDocument.class);
      return document.content() == null ? List.of() : document.content();
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse content YAML", e);
    }
  }

  private static boolean isLogoFile(String name) {
    if (name == null) {
      return false;
    }
    var lower = name.toLowerCase(Locale.ROOT);
    return lower.endsWith(".svg") || lower.endsWith(".png");
  }
}
