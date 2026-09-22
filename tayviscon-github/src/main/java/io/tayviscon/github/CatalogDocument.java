package io.tayviscon.github;

import java.util.List;

/** YAML-документ {@code catalog.yaml}. */
public record CatalogDocument(String series, List<CourseEntry> courses) {
  /** Строка курса из {@code catalog.yaml}. */
  public record CourseEntry(String id, String title, String path, String status) {}
}
