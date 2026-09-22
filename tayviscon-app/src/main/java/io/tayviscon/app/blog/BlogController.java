package io.tayviscon.app.blog;

import io.tayviscon.core.blog.BlogArticle;
import io.tayviscon.core.blog.BlogCatalog;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API публичного блога: дерево, лента, статья и ассеты. */
@RestController
@RequestMapping("/api/blog")
public class BlogController {
  private final BlogCatalog catalog;

  /** Создаёт контроллер поверх каталога блога. */
  public BlogController(BlogCatalog catalog) {
    this.catalog = catalog;
  }

  /** Дерево опубликованных статей по группам. */
  @GetMapping("/tree")
  public Map<String, Object> tree() {
    return Map.of("groups", catalog.tree());
  }

  /** До двух последних постов для главной. */
  @GetMapping("/latest")
  public Map<String, Object> latest() {
    return Map.of("posts", catalog.latest());
  }

  /** Статья по пути в репозитории knowledge-base или 404. */
  @GetMapping("/articles/{*path}")
  public ResponseEntity<BlogArticle> article(@PathVariable String path) {
    return catalog
        .article(strip(path))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /** Байты ассета опубликованной статьи или 404. */
  @GetMapping("/assets/{*path}")
  public ResponseEntity<byte[]> asset(@PathVariable String path) {
    return catalog
        .asset(strip(path))
        .map(
            a ->
                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(a.contentType()))
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                    .body(a.bytes()))
        .orElse(ResponseEntity.notFound().build());
  }

  private static String strip(String path) {
    return path != null && path.startsWith("/") ? path.substring(1) : path;
  }
}
