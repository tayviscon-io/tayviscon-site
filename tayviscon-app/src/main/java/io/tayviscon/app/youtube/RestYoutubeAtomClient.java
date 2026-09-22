package io.tayviscon.app.youtube;

import io.tayviscon.core.youtube.YoutubeUnavailableException;
import java.time.Duration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Скачивает публичную Atom-ленту канала YouTube по HTTP. */
public class RestYoutubeAtomClient implements YoutubeAtomClient {
  static final String FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id={id}";
  static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
  static final Duration READ_TIMEOUT = Duration.ofSeconds(8);

  private final RestClient restClient;
  private final String channelId;

  /** Создаёт клиент для {@code channelId} с таймаутами по умолчанию. */
  public RestYoutubeAtomClient(String channelId) {
    this(RestClient.builder().requestFactory(timedRequestFactory()).build(), channelId);
  }

  RestYoutubeAtomClient(RestClient restClient, String channelId) {
    this.restClient = restClient;
    this.channelId = channelId;
  }

  @Override
  public String fetchAtom() {
    try {
      String body =
          restClient
              .get()
              .uri(FEED_URL, channelId)
              .header("User-Agent", "TayvisconSite/0.1")
              .header("Accept", "application/atom+xml, application/xml, text/xml")
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  (req, res) -> {
                    throw new YoutubeUnavailableException(
                        "YouTube Atom HTTP " + res.getStatusCode().value(), null);
                  })
              .body(String.class);
      if (body == null || body.isBlank()) {
        throw new YoutubeUnavailableException("YouTube Atom empty", null);
      }
      return body;
    } catch (YoutubeUnavailableException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new YoutubeUnavailableException("YouTube Atom fetch failed", e);
    }
  }

  static SimpleClientHttpRequestFactory timedRequestFactory() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT);
    factory.setReadTimeout(READ_TIMEOUT);
    return factory;
  }
}
