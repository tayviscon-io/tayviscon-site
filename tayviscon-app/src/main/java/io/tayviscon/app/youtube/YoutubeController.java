package io.tayviscon.app.youtube;

import io.tayviscon.core.youtube.YoutubeFeed;
import io.tayviscon.core.youtube.YoutubeVideo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-эндпоинты последних видео YouTube. */
@RestController
@RequestMapping("/api/youtube")
public class YoutubeController {
  private final YoutubeFeed feed;

  /** Создаёт контроллер поверх {@code feed}. */
  public YoutubeController(YoutubeFeed feed) {
    this.feed = feed;
  }

  /** Возвращает последние закэшированные видео. */
  @GetMapping("/latest")
  public YoutubeListResponse latest() {
    return new YoutubeListResponse(feed.latest());
  }

  /** JSON-обёртка для эндпоинта последних видео. */
  public record YoutubeListResponse(List<YoutubeVideo> videos) {}
}
