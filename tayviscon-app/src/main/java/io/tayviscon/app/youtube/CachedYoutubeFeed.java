package io.tayviscon.app.youtube;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.tayviscon.core.youtube.YoutubeFeed;
import io.tayviscon.core.youtube.YoutubeUnavailableException;
import io.tayviscon.core.youtube.YoutubeVideo;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link YoutubeFeed} с недельным кэшем и последним удачным снимком. */
public class CachedYoutubeFeed implements YoutubeFeed {
  private static final Logger log = LoggerFactory.getLogger(CachedYoutubeFeed.class);
  private static final String CACHE_KEY = "latest";
  static final int LIMIT = 5;

  private final YoutubeAtomClient client;
  private final AtomYoutubeFeedParser parser;
  private final LoadingCache<String, List<YoutubeVideo>> cache;
  private volatile List<YoutubeVideo> lastGood;

  /** Создаёт ленту без начального снимка. */
  public CachedYoutubeFeed(YoutubeAtomClient client, AtomYoutubeFeedParser parser) {
    this(client, parser, List.of());
  }

  /** Создаёт ленту, которая может отдать {@code seed}, если живой Atom-запрос не удался. */
  public CachedYoutubeFeed(
      YoutubeAtomClient client, AtomYoutubeFeedParser parser, List<YoutubeVideo> seed) {
    this.client = client;
    this.parser = parser;
    if (seed != null && !seed.isEmpty()) {
      this.lastGood = List.copyOf(seed);
    }
    this.cache = Caffeine.newBuilder().expireAfterWrite(7, TimeUnit.DAYS).build(key -> load());
    if (this.lastGood != null) {
      this.cache.put(CACHE_KEY, this.lastGood);
    }
  }

  @Override
  public List<YoutubeVideo> latest() {
    try {
      return cache.get(CACHE_KEY);
    } catch (RuntimeException e) {
      if (lastGood != null) {
        log.warn("YouTube feed fetch failed; serving last-good snapshot", e);
        return lastGood;
      }
      if (e instanceof YoutubeUnavailableException unavailable) {
        throw unavailable;
      }
      throw new YoutubeUnavailableException("YouTube feed unavailable", e);
    }
  }

  void evict() {
    cache.invalidate(CACHE_KEY);
  }

  private List<YoutubeVideo> load() {
    try {
      var snapshot = parser.parse(client.fetchAtom(), LIMIT);
      if (snapshot.isEmpty() && lastGood != null) {
        log.warn("YouTube feed empty; keeping last-good snapshot");
        return lastGood;
      }
      lastGood = snapshot;
      return snapshot;
    } catch (RuntimeException e) {
      if (lastGood != null) {
        log.warn("YouTube feed fetch failed; serving last-good snapshot", e);
        return lastGood;
      }
      if (e instanceof YoutubeUnavailableException unavailable) {
        throw unavailable;
      }
      throw new YoutubeUnavailableException("YouTube feed unavailable", e);
    }
  }
}
