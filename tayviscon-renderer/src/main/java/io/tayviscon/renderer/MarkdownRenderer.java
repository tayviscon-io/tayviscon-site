package io.tayviscon.renderer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.DefaultUrlSanitizer;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Преобразует Markdown в HTML: документ курса или фрагмент статьи блога с YouTube и
 * переписью относительных картинок на {@code /api/blog/assets/…}.
 */
public class MarkdownRenderer {
  private static final Pattern RELATIVE_IMG =
      Pattern.compile("(<img\\b[^>]*\\bsrc=\")(?:\\./)?((?![/])(?![^/]*:)[^\"]+)(\")");
  private static final Pattern YT_WATCH =
      Pattern.compile(
          "(?is)<p>\\s*(?:<a[^>]+href=\"https://(?:www\\.)?youtube\\.com/watch\\?v=([\\w-]+)[^\"]*\"[^>]*>.*?</a>|https://(?:www\\.)?youtube\\.com/watch\\?v=([\\w-]+)(?:[&?][^\\s<]*)?\\s*)\\s*</p>");
  private static final Pattern YT_BE =
      Pattern.compile(
          "(?is)<p>\\s*(?:<a[^>]+href=\"https://youtu\\.be/([\\w-]+)[^\"]*\"[^>]*>.*?</a>|https://youtu\\.be/([\\w-]+)(?:\\?[^\\s<]*)?\\s*)\\s*</p>");

  private final Parser parser = Parser.builder().build();
  private final HtmlRenderer renderer =
      HtmlRenderer.builder()
          .escapeHtml(true)
          .sanitizeUrls(true)
          .urlSanitizer(new DefaultUrlSanitizer(List.of("http", "https", "mailto")))
          .build();

  /**
   * Конвертирует Markdown в HTML-документ: экранирует сырой HTML и разрешает только URL
   * http(s)/mailto.
   */
  public String toHtml(String markdown) {
    return "<html><body>" + toFragment(markdown, "") + "</body></html>";
  }

  /**
   * Конвертирует Markdown во фрагмент без {@code <html>}/{@code <body>}: YouTube → iframe,
   * относительные картинки → {@code /api/blog/assets/{articlePath}/…}.
   */
  public String toFragment(String markdown, String articlePath) {
    Node document = parser.parse(markdown == null ? "" : markdown);
    String html = renderer.render(document);
    html = replaceYoutube(html);
    if (articlePath != null && !articlePath.isBlank()) {
      html = rewriteRelativeImages(html, articlePath);
    }
    return html;
  }

  private static String replaceYoutube(String html) {
    html = replacePattern(html, YT_WATCH);
    html = replacePattern(html, YT_BE);
    return html;
  }

  private static String replacePattern(String html, Pattern pattern) {
    Matcher matcher = pattern.matcher(html);
    StringBuffer out = new StringBuffer();
    while (matcher.find()) {
      String id = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
      matcher.appendReplacement(out, Matcher.quoteReplacement(iframe(id)));
    }
    matcher.appendTail(out);
    return out.toString();
  }

  private static String iframe(String id) {
    return "<iframe class=\"yt-embed\" src=\"https://www.youtube.com/embed/"
        + id
        + "\" title=\"YouTube\" allowfullscreen></iframe>";
  }

  private static String rewriteRelativeImages(String html, String articlePath) {
    Matcher matcher = RELATIVE_IMG.matcher(html);
    StringBuffer out = new StringBuffer();
    while (matcher.find()) {
      String file = matcher.group(2);
      String src =
          matcher.group(1)
              + "/api/blog/assets/"
              + encodePathSegments(articlePath)
              + "/"
              + file
              + matcher.group(3);
      matcher.appendReplacement(out, Matcher.quoteReplacement(src));
    }
    matcher.appendTail(out);
    return out.toString();
  }

  private static String encodePathSegments(String path) {
    String[] segments = path.split("/", -1);
    StringBuilder encoded = new StringBuilder();
    for (int i = 0; i < segments.length; i++) {
      if (i > 0) {
        encoded.append('/');
      }
      encoded.append(
          URLEncoder.encode(segments[i], StandardCharsets.UTF_8).replace("+", "%20"));
    }
    return encoded.toString();
  }
}
