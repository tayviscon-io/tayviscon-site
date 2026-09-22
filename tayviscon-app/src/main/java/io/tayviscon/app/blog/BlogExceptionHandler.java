package io.tayviscon.app.blog;

import io.tayviscon.core.blog.BlogUnavailableException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Преобразует недоступность блога в HTTP 503 с русским сообщением. */
@RestControllerAdvice
public class BlogExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(BlogExceptionHandler.class);

  /** Отдаёт 503, если каталог блога не смог прочитать GitHub. */
  @ExceptionHandler(BlogUnavailableException.class)
  public ResponseEntity<Map<String, String>> handleBlogUnavailable(
      BlogUnavailableException exception) {
    log.error("Blog unavailable; returning 503", exception);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("error", "блог временно недоступен"));
  }
}
