package io.tayviscon.app.youtube;

import io.tayviscon.core.youtube.YoutubeFeed;
import io.tayviscon.core.youtube.YoutubeVideo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Собирает кэшированную ленту YouTube и необязательный seed из classpath. */
@Configuration
public class YoutubeConfig {
  private static final Logger log = LoggerFactory.getLogger(YoutubeConfig.class);

  @Bean
  YoutubeFeed youtubeFeed(
      @Value("${youtube.channel-id:UCZhzhQbtFdKturwa_7FGIMA}") String channelId) {
    var parser = new AtomYoutubeFeedParser();
    return new CachedYoutubeFeed(
        new RestYoutubeAtomClient(channelId), parser, loadSeed(parser));
  }

  static List<YoutubeVideo> loadSeed(AtomYoutubeFeedParser parser) {
    try (var stream = YoutubeConfig.class.getResourceAsStream("/youtube-seed.atom.xml")) {
      if (stream == null) {
        return List.of();
      }
      return parser.parse(
          new String(stream.readAllBytes(), StandardCharsets.UTF_8), CachedYoutubeFeed.LIMIT);
    } catch (IOException e) {
      log.warn("YouTube seed could not be read", e);
      return List.of();
    }
  }
}
