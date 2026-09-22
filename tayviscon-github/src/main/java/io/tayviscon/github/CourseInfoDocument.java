package io.tayviscon.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * YAML-документ {@code course-info.yaml}, {@code section-info.yaml} и
 * {@code lesson-info.yaml}.
 */
public record CourseInfoDocument(
    String title,
    String summary,
    List<String> content,
    @JsonProperty("additional_files") List<AdditionalFile> additionalFiles) {
  /** Дополнительный файл из списка {@code additional_files}. */
  public record AdditionalFile(String name) {}
}
