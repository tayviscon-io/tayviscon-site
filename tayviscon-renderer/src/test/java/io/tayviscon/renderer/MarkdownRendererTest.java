package io.tayviscon.renderer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarkdownRendererTest {
  private final MarkdownRenderer renderer = new MarkdownRenderer();

  @Test
  void rendersHeading() {
    String html = renderer.toHtml("# Hi");
    assertTrue(html.contains("<h1>"));
  }

  @Test
  void fragmentHasNoDocumentChrome() {
    String html = renderer.toFragment("# Hi", "systems/online-code-execution");
    assertFalse(html.contains("<html>"));
    assertFalse(html.contains("<body>"));
    assertTrue(html.contains("<h1>Hi</h1>"));
  }

  @Test
  void rewritesRelativeImageToBlogAsset() {
    String html =
        renderer.toFragment(
            "![движок](./engine.excalidraw.svg)", "systems/online-code-execution");
    assertTrue(
        html.contains(
            "src=\"/api/blog/assets/systems/online-code-execution/engine.excalidraw.svg\""));
    assertTrue(html.contains("alt=\"движок\""));
  }

  @Test
  void leavesHttpsImagesUntouched() {
    String html =
        renderer.toFragment("![x](https://example.com/a.png)", "systems/online-code-execution");
    assertTrue(html.contains("src=\"https://example.com/a.png\""));
    assertFalse(html.contains("/api/blog/assets/"));
  }

  @Test
  void leavesHttpImagesUntouched() {
    String html =
        renderer.toFragment("![x](http://example.com/a.png)", "systems/online-code-execution");
    assertTrue(html.contains("src=\"http://example.com/a.png\""));
    assertFalse(html.contains("/api/blog/assets/"));
  }

  @Test
  void leavesUppercaseHttpsImagesUntouched() {
    String html =
        renderer.toFragment("![x](HTTPS://example.com/a.png)", "systems/online-code-execution");
    assertTrue(html.contains("src=\"HTTPS://example.com/a.png\""));
    assertFalse(html.contains("/api/blog/assets/"));
  }

  @Test
  void leavesRootRelativeImagesUntouched() {
    String html =
        renderer.toFragment("![x](/images/a.png)", "systems/online-code-execution");
    assertTrue(html.contains("src=\"/images/a.png\""));
    assertFalse(html.contains("/api/blog/assets/"));
  }

  @Test
  void leavesProtocolRelativeImagesUntouched() {
    String html =
        renderer.toFragment("![x](//cdn.example/a.png)", "systems/online-code-execution");
    assertTrue(html.contains("src=\"//cdn.example/a.png\""));
    assertFalse(html.contains("/api/blog/assets/"));
  }

  @Test
  void sanitizesDataUriImages() {
    String html =
        renderer.toFragment(
            "![x](data:image/png;base64,iVBORw0KGgo=)", "systems/online-code-execution");
    assertFalse(html.contains("data:image/png"));
    assertFalse(html.contains("/api/blog/assets/"));
  }

  @Test
  void watchUrlParagraphBecomesIframe() {
    String html =
        renderer.toFragment(
            "https://www.youtube.com/watch?v=lhGamS89atg", "systems/demo");
    assertTrue(html.contains("youtube.com/embed/lhGamS89atg"));
    assertTrue(html.contains("<iframe"));
    assertFalse(html.contains("<p>https://www.youtube.com/watch"));
  }

  @Test
  void youtuBeParagraphBecomesIframe() {
    String html = renderer.toFragment("https://youtu.be/lhGamS89atg", "systems/demo");
    assertTrue(html.contains("youtube.com/embed/lhGamS89atg"));
  }

  @Test
  void watchUrlWithQueryParamsBecomesIframe() {
    String html =
        renderer.toFragment(
            "https://www.youtube.com/watch?v=lhGamS89atg&t=30", "systems/demo");
    assertTrue(html.contains("youtube.com/embed/lhGamS89atg"));
    assertTrue(html.contains("<iframe"));
    assertFalse(html.contains("<p>https://www.youtube.com/watch"));
  }

  @Test
  void youtuBeUrlWithQueryParamsBecomesIframe() {
    String html =
        renderer.toFragment("https://youtu.be/lhGamS89atg?si=abc123", "systems/demo");
    assertTrue(html.contains("youtube.com/embed/lhGamS89atg"));
    assertTrue(html.contains("<iframe"));
    assertFalse(html.contains("<p>https://youtu.be/"));
  }

  @Test
  void encodesArticlePathInImageSrc() {
    String html =
        renderer.toFragment("![x](./a.png)", "systems/foo bar/\"evil\"");
    assertTrue(
        html.contains(
            "src=\"/api/blog/assets/systems/foo%20bar/%22evil%22/a.png\""));
    assertFalse(html.contains("foo bar"));
  }

  @Test
  void stripsRawHtmlIframe() {
    String html = renderer.toFragment("<iframe src=\"https://evil.example\"></iframe>", "s/d");
    assertFalse(html.contains("<iframe src=\"https://evil.example\""));
    assertTrue(html.contains("&lt;iframe"));
  }

  @Test
  void youtuBeUrlWithTrailingProseStaysParagraph() {
    String html =
        renderer.toFragment("https://youtu.be/lhGamS89atg explanatory text", "systems/demo");
    assertFalse(html.contains("<iframe"));
    assertTrue(html.contains("https://youtu.be/lhGamS89atg"));
    assertTrue(html.contains("explanatory text"));
  }

  @Test
  void watchUrlWithTrailingProseStaysParagraph() {
    String html =
        renderer.toFragment(
            "https://www.youtube.com/watch?v=lhGamS89atg explanatory text", "systems/demo");
    assertFalse(html.contains("<iframe"));
    assertTrue(html.contains("https://www.youtube.com/watch?v=lhGamS89atg"));
    assertTrue(html.contains("explanatory text"));
  }

  @Test
  void preservesEncodedSpacesInImageFilename() {
    String html = renderer.toFragment("![x](./my%20image.png)", "systems/demo");
    assertTrue(html.contains("src=\"/api/blog/assets/systems/demo/my%20image.png\""));
    assertFalse(html.contains("%2520"));
  }

  @Test
  void rewritesBareRelativeImage() {
    String html = renderer.toFragment("![x](a.png)", "systems/demo");
    assertTrue(html.contains("src=\"/api/blog/assets/systems/demo/a.png\""));
  }

  @Test
  void rewritesNestedRelativeImage() {
    String html = renderer.toFragment("![x](dir/file.png)", "systems/demo");
    assertTrue(html.contains("src=\"/api/blog/assets/systems/demo/dir/file.png\""));
  }

  @Test
  void preservesQueryStringInImageSrc() {
    String html = renderer.toFragment("![x](./a.png?x=1&y=2)", "systems/demo");
    assertTrue(
        html.contains("src=\"/api/blog/assets/systems/demo/a.png?x=1&amp;y=2\""));
    assertFalse(html.contains("&amp;amp;"));
  }

  @Test
  void javascriptLinkHrefIsSanitized() {
    String html = renderer.toFragment("[x](javascript:alert(1))", "systems/demo");
    assertFalse(html.contains("href=\"javascript:"));
    assertFalse(html.contains("href='javascript:"));
  }
}
