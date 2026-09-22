package io.tayviscon.app.course;

import io.tayviscon.core.course.CourseCatalog;
import io.tayviscon.core.course.LogoAsset;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-эндпоинты публичного каталога курсов. */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

  private final CourseCatalog catalog;

  /** Создаёт контроллер поверх {@code catalog}. */
  public CourseController(CourseCatalog catalog) {
    this.catalog = catalog;
  }

  /** Возвращает все курсы каталога. */
  @GetMapping
  public CourseListResponse list() {
    List<CourseApi> courses = catalog.list().stream().map(CourseApi::from).toList();
    return new CourseListResponse(courses);
  }

  /** Возвращает один курс или 404, если {@code id} неизвестен. */
  @GetMapping("/{id}")
  public ResponseEntity<CourseApi> findById(@PathVariable String id) {
    return catalog
        .findById(id)
        .map(CourseApi::fromDetail)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /** Возвращает логотип курса как вложение или 404, если его нет. */
  @GetMapping("/{id}/logo")
  public ResponseEntity<byte[]> logo(@PathVariable String id) {
    return catalog
        .logo(id)
        .map(CourseController::toLogoResponse)
        .orElse(ResponseEntity.notFound().build());
  }

  private static ResponseEntity<byte[]> toLogoResponse(LogoAsset logo) {
    if (logo == null || logo.bytes() == null) {
      return ResponseEntity.notFound().build();
    }
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(logo.contentType()));
    headers.setCacheControl(CacheControl.maxAge(Duration.ofDays(7)));
    headers.setContentDisposition(ContentDisposition.attachment().filename("logo").build());
    if ("image/svg+xml".equalsIgnoreCase(logo.contentType())) {
      headers.set("Content-Security-Policy", "sandbox; default-src 'none'");
    }
    return new ResponseEntity<>(logo.bytes(), headers, HttpStatus.OK);
  }

  /** JSON-обёртка для списка курсов. */
  public record CourseListResponse(List<CourseApi> courses) {}
}
