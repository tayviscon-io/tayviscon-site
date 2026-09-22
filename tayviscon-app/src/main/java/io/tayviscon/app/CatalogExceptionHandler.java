package io.tayviscon.app;

import io.tayviscon.core.course.CatalogUnavailableException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Преобразует сбои каталога в HTTP 503. */
@RestControllerAdvice
public class CatalogExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(CatalogExceptionHandler.class);

  /** Возвращает JSON с ошибкой, если каталог GitHub не загрузился. */
  @ExceptionHandler(CatalogUnavailableException.class)
  public ResponseEntity<Map<String, String>> handleCatalogUnavailable(
      CatalogUnavailableException exception) {
    log.error("Catalog unavailable; returning 503", exception);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("error", "каталог временно недоступен"));
  }
}
