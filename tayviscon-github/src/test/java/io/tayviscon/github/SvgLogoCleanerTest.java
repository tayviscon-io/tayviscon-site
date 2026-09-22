package io.tayviscon.github;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SvgLogoCleanerTest {
  @Test
  void stripsFullCanvasRectKeepsInnerWhite() {
    String input =
        """
        <svg viewBox="0 0 200 100" xmlns="http://www.w3.org/2000/svg">
          <rect width="200" height="100" fill="#ffffff"/>
          <rect x="10" y="10" width="8" height="8" fill="#ffffff"/>
        </svg>
        """;
    String out = new SvgLogoCleaner().clean(input);
    assertFalse(out.contains("width=\"200\" height=\"100\""));
    assertTrue(out.contains("width=\"8\" height=\"8\""));
  }

  @Test
  void stripsScriptEventHandlersAndUnsafeLinks() {
    String input =
        """
        <svg viewBox="0 0 10 10" xmlns="http://www.w3.org/2000/svg"
             xmlns:xlink="http://www.w3.org/1999/xlink">
          <script>alert(1)</script>
          <rect width="2" height="2" fill="red" onclick="alert(1)"/>
          <a href="javascript:alert(1)" xlink:href="https://evil.example/x">x</a>
        </svg>
        """;
    String out = new SvgLogoCleaner().clean(input);
    assertNotNull(out);
    assertFalse(out.toLowerCase().contains("<script"));
    assertFalse(out.toLowerCase().contains("onclick"));
    assertFalse(out.toLowerCase().contains("javascript:"));
    assertFalse(out.contains("https://evil.example/x"));
  }

  @Test
  void blankBecomesNull() {
    assertNull(new SvgLogoCleaner().clean("  "));
    assertNull(new SvgLogoCleaner().clean(null));
    assertNull(new SvgLogoCleaner().clean(""));
  }

  @Test
  void stripsCanvasFromExcalidrawFixture() throws IOException {
    String input = load("excalidraw-canvas.svg");
    String out = new SvgLogoCleaner().clean(input);
    assertNotNull(out);
    assertFalse(out.contains("width=\"800\" height=\"600\""));
    assertTrue(out.contains("width=\"12\" height=\"12\""));
  }

  private static String load(String resource) throws IOException {
    try (var stream = SvgLogoCleanerTest.class.getClassLoader().getResourceAsStream(resource)) {
      assertNotNull(stream, resource);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
