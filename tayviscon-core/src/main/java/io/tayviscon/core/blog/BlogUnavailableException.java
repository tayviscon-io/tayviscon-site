package io.tayviscon.core.blog;

/** Блог недоступен: GitHub или разбор снимка не удались, last-good тоже пуст. */
public class BlogUnavailableException extends RuntimeException {
  /** Создаёт исключение с причиной. */
  public BlogUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
