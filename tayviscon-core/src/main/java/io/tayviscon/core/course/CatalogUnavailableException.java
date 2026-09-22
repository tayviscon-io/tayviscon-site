package io.tayviscon.core.course;

/** Сигнализирует, что каталог курсов не удалось загрузить с GitHub. */
public class CatalogUnavailableException extends RuntimeException {
  /** Создаёт исключение с сообщением и причиной. */
  public CatalogUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
