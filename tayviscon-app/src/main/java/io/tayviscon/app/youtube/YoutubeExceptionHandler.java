package io.tayviscon.app.youtube;

import io.tayviscon.core.youtube.YoutubeUnavailableException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Преобразует сбои ленты YouTube в HTTP 503. */
@RestControllerAdvice
public class YoutubeExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(YoutubeExceptionHandler.class);

  /** Возвращает JSON с ошибкой, если Atom-ленту нельзя отдать. */
  @ExceptionHandler(YoutubeUnavailableException.class)
  public ResponseEntity<Map<String, String>> handleUnavailable(
      YoutubeUnavailableException exception) {
    log.error("YouTube feed unavailable; returning 503", exception);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("error", "канал временно недоступен"));
  }
}
