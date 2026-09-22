package io.tayviscon.github;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Убирает canvas-прямоугольники и исполняемую разметку из SVG-логотипов курсов. */
public class SvgLogoCleaner {
  private static final double TOLERANCE = 1.0;
  private static final Pattern VIEW_BOX =
      Pattern.compile("viewBox\\s*=\\s*\"[^\"]*\\s+(\\d+(?:\\.\\d+)?)\\s+(\\d+(?:\\.\\d+)?)\"");
  private static final Pattern RECT_TAG =
      Pattern.compile("<rect\\b[^>]*(?:/>|>\\s*</rect>)", Pattern.CASE_INSENSITIVE);
  private static final Pattern WIDTH = Pattern.compile("\\bwidth\\s*=\\s*\"(\\d+(?:\\.\\d+)?)\"");
  private static final Pattern HEIGHT = Pattern.compile("\\bheight\\s*=\\s*\"(\\d+(?:\\.\\d+)?)\"");
  private static final Pattern FILL = Pattern.compile("\\bfill\\s*=\\s*\"([^\"]+)\"");
  private static final Pattern SCRIPT =
      Pattern.compile("(?is)<script\\b[^>]*>.*?</script>|<script\\b[^>]*/>");
  private static final Pattern EVENT_HANDLER =
      Pattern.compile("(?i)\\s+on[a-z]+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)");
  private static final Pattern JAVASCRIPT_HREF =
      Pattern.compile("(?i)\\s+(?:xlink:)?href\\s*=\\s*([\"'])\\s*javascript:[^\"']*\\1");
  private static final Pattern EXTERNAL_XLINK =
      Pattern.compile("(?i)\\s+xlink:href\\s*=\\s*([\"'])\\s*(?:https?:)?//[^\"']*\\1");

  /**
   * Возвращает очищенный SVG или {@code null}, если вход пустой.
   */
  public String clean(String svg) {
    if (svg == null || svg.isBlank()) {
      return null;
    }
    return sanitize(stripCanvasRect(svg));
  }

  private static String stripCanvasRect(String svg) {
    Matcher viewBoxMatcher = VIEW_BOX.matcher(svg);
    if (!viewBoxMatcher.find()) {
      return svg;
    }
    double vbWidth = Double.parseDouble(viewBoxMatcher.group(1));
    double vbHeight = Double.parseDouble(viewBoxMatcher.group(2));

    Matcher rectMatcher = RECT_TAG.matcher(svg);
    StringBuilder result = new StringBuilder();
    int last = 0;
    while (rectMatcher.find()) {
      String rect = rectMatcher.group();
      if (isWhiteCanvasRect(rect, vbWidth, vbHeight)) {
        result.append(svg, last, rectMatcher.start());
        last = rectMatcher.end();
      }
    }
    result.append(svg, last, svg.length());
    return result.toString();
  }

  private static String sanitize(String svg) {
    String cleaned = SCRIPT.matcher(svg).replaceAll("");
    cleaned = EVENT_HANDLER.matcher(cleaned).replaceAll("");
    cleaned = JAVASCRIPT_HREF.matcher(cleaned).replaceAll("");
    return EXTERNAL_XLINK.matcher(cleaned).replaceAll("");
  }

  private static boolean isWhiteCanvasRect(String rect, double vbWidth, double vbHeight) {
    Double width = parseDimension(WIDTH, rect);
    Double height = parseDimension(HEIGHT, rect);
    if (width == null || height == null) {
      return false;
    }
    if (Math.abs(width - vbWidth) > TOLERANCE || Math.abs(height - vbHeight) > TOLERANCE) {
      return false;
    }
    Matcher fillMatcher = FILL.matcher(rect);
    if (!fillMatcher.find()) {
      return false;
    }
    return isWhiteFill(fillMatcher.group(1));
  }

  private static Double parseDimension(Pattern pattern, String rect) {
    Matcher matcher = pattern.matcher(rect);
    if (!matcher.find()) {
      return null;
    }
    return Double.parseDouble(matcher.group(1));
  }

  private static boolean isWhiteFill(String fill) {
    return switch (fill.trim().toLowerCase()) {
      case "#ffffff", "#fff", "white" -> true;
      default -> false;
    };
  }
}
