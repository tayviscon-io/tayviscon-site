package io.tayviscon.core.course;

/** Состояние публикации курса, как оно показано на сайте. */
public enum CourseStatus {
  IN_PROGRESS, PUBLISHED, FINISHED;

  /**
   * Разбирает статус из YAML каталога, включая устаревшие {@code stub} и {@code content}.
   */
  public static CourseStatus fromCatalog(String raw) {
    return switch (raw) {
      case "in_progress", "IN_PROGRESS" -> IN_PROGRESS;
      case "published", "PUBLISHED" -> PUBLISHED;
      case "finished", "FINISHED" -> FINISHED;
      case "stub" -> IN_PROGRESS;
      case "content" -> PUBLISHED;
      default -> throw new IllegalArgumentException("Unknown course status: " + raw);
    };
  }

  /** Возвращает русскую подпись бейджа для этого статуса. */
  public String badge() {
    return switch (this) {
      case IN_PROGRESS -> "в работе";
      case PUBLISHED -> "доступен";
      case FINISHED -> "завершён";
    };
  }
}
