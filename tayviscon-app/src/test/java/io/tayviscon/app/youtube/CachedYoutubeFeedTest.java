package io.tayviscon.app.youtube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.tayviscon.core.youtube.YoutubeUnavailableException;
import org.junit.jupiter.api.Test;

class CachedYoutubeFeedTest {
  private static final String FEED =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015"
            xmlns="http://www.w3.org/2005/Atom">
        <entry>
          <yt:videoId>lhGamS89atg</yt:videoId>
          <title>Tayviscon: Рождение из хаоса</title>
          <published>2024-08-05T18:04:19+00:00</published>
        </entry>
      </feed>
      """;

  @Test
  void servesLastGoodWhenFetchFails() {
    var client = new FakeAtom();
    client.xml = FEED;
    var feed = new CachedYoutubeFeed(client, new AtomYoutubeFeedParser());
    assertEquals("lhGamS89atg", feed.latest().getFirst().id());
    final int afterOk = client.calls;
    feed.evict();
    client.fail = true;
    assertEquals("lhGamS89atg", feed.latest().getFirst().id());
    assertTrue(client.calls > afterOk);
  }

  @Test
  void throwsWhenNoSnapshot() {
    var client = new FakeAtom();
    client.fail = true;
    var feed = new CachedYoutubeFeed(client, new AtomYoutubeFeedParser());
    assertThrows(YoutubeUnavailableException.class, feed::latest);
  }

  @Test
  void emptySuccessDoesNotReplaceSeed() {
    var client = new FakeAtom();
    client.xml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015"
              xmlns="http://www.w3.org/2005/Atom">
        </feed>
        """;
    var seed = new AtomYoutubeFeedParser().parse(FEED, 5);
    var feed = new CachedYoutubeFeed(client, new AtomYoutubeFeedParser(), seed);
    feed.evict();
    assertEquals("lhGamS89atg", feed.latest().getFirst().id());
  }

  @Test
  void seedServesWhenFetchNeverSucceeds() {
    var client = new FakeAtom();
    client.fail = true;
    var seed =
        new AtomYoutubeFeedParser()
            .parse(FEED, 5);
    var feed = new CachedYoutubeFeed(client, new AtomYoutubeFeedParser(), seed);
    assertEquals("lhGamS89atg", feed.latest().getFirst().id());
  }

  static final class FakeAtom implements YoutubeAtomClient {
    String xml = "";
    boolean fail;
    int calls;

    @Override
    public String fetchAtom() {
      calls++;
      if (fail) {
        throw new YoutubeUnavailableException("down", null);
      }
      return xml;
    }
  }
}
