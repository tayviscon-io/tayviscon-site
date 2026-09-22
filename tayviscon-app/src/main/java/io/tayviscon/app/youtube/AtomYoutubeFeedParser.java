package io.tayviscon.app.youtube;

import io.tayviscon.core.youtube.YoutubeUnavailableException;
import io.tayviscon.core.youtube.YoutubeVideo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Разбирает Atom XML канала YouTube в записи {@link YoutubeVideo}. */
public class AtomYoutubeFeedParser {
  private static final String ATOM = "http://www.w3.org/2005/Atom";
  private static final String YT = "http://www.youtube.com/xml/schemas/2015";
  private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

  /** Разбирает не больше {@code limit} записей из Atom-документа канала. */
  public List<YoutubeVideo> parse(String atomXml, int limit) {
    if (atomXml == null || atomXml.isBlank()) {
      return List.of();
    }
    try {
      var factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      var document =
          factory
              .newDocumentBuilder()
              .parse(new ByteArrayInputStream(atomXml.getBytes(StandardCharsets.UTF_8)));
      NodeList entries = document.getElementsByTagNameNS(ATOM, "entry");
      List<YoutubeVideo> videos = new ArrayList<>();
      for (int i = 0; i < entries.getLength() && videos.size() < limit; i++) {
        var entry = (Element) entries.item(i);
        String id = text(entry, YT, "videoId");
        if (id.isBlank()) {
          continue;
        }
        videos.add(
            new YoutubeVideo(
                id,
                text(entry, ATOM, "title"),
                "https://www.youtube.com/watch?v=" + id,
                day(text(entry, ATOM, "published")),
                "https://i.ytimg.com/vi/" + id + "/mqdefault.jpg"));
      }
      return List.copyOf(videos);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new YoutubeUnavailableException("YouTube Atom parse failed", e);
    }
  }

  private static String text(Element entry, String ns, String localName) {
    NodeList nodes = entry.getElementsByTagNameNS(ns, localName);
    if (nodes.getLength() == 0) {
      return "";
    }
    return nodes.item(0).getTextContent().trim();
  }

  private static String day(String published) {
    if (published.isBlank()) {
      return "";
    }
    try {
      return Instant.parse(published).atOffset(ZoneOffset.UTC).toLocalDate().format(DAY);
    } catch (DateTimeParseException e) {
      return published.length() >= 10 ? published.substring(0, 10) : published;
    }
  }
}
