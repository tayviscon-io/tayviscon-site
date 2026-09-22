package io.tayviscon.app.youtube;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AtomYoutubeFeedParserTest {
  private static final String FEED =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015"
            xmlns="http://www.w3.org/2005/Atom">
        <title>Tayviscon</title>
        <entry>
          <yt:videoId>oneAAAAAAA1</yt:videoId>
          <title>First</title>
          <published>2024-08-05T18:04:19+00:00</published>
        </entry>
        <entry>
          <yt:videoId>twoBBBBBBB2</yt:videoId>
          <title>Second</title>
          <published>2024-05-19T09:53:11+00:00</published>
        </entry>
        <entry>
          <yt:videoId>threeCCCCC3</yt:videoId>
          <title>Third</title>
          <published>2024-04-01T00:00:00+00:00</published>
        </entry>
        <entry>
          <yt:videoId>fourDDDDDD4</yt:videoId>
          <title>Fourth</title>
          <published>2024-03-01T00:00:00+00:00</published>
        </entry>
        <entry>
          <yt:videoId>fiveEEEEEE5</yt:videoId>
          <title>Fifth</title>
          <published>2024-02-01T00:00:00+00:00</published>
        </entry>
        <entry>
          <yt:videoId>sixFFFFFFF6</yt:videoId>
          <title>Sixth should drop</title>
          <published>2024-01-01T00:00:00+00:00</published>
        </entry>
      </feed>
      """;

  @Test
  void keepsFiveNewestAndBuildsMqdefaultThumbs() {
    var videos = new AtomYoutubeFeedParser().parse(FEED, 5);
    assertEquals(5, videos.size());
    assertEquals("oneAAAAAAA1", videos.get(0).id());
    assertEquals("First", videos.get(0).title());
    assertEquals("https://www.youtube.com/watch?v=oneAAAAAAA1", videos.get(0).url());
    assertEquals("2024-08-05", videos.get(0).published());
    assertEquals(
        "https://i.ytimg.com/vi/oneAAAAAAA1/mqdefault.jpg", videos.get(0).thumbnailUrl());
    assertEquals("fiveEEEEEE5", videos.get(4).id());
  }
}
