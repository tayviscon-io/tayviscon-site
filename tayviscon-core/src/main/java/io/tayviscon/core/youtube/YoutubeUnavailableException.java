package io.tayviscon.core.youtube;

/** Сигнализирует, что Atom-ленту YouTube не удалось получить или разобрать. */
public class YoutubeUnavailableException extends RuntimeException {
  /** Создаёт исключение с сообщением и причиной. */
  public YoutubeUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
