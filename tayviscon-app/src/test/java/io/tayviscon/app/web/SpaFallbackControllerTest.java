package io.tayviscon.app.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.tayviscon.core.blog.BlogCatalog;
import io.tayviscon.core.course.Course;
import io.tayviscon.core.course.CourseCatalog;
import io.tayviscon.core.course.CourseStatus;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SpaFallbackControllerTest {

  private static final Pattern HASHED_JS =
      Pattern.compile("src=\"(/assets/[^\"]+\\.js)\"");

  @Autowired MockMvc mvc;

  @Autowired TestRestTemplate rest;

  @MockitoBean CourseCatalog catalog;

  @MockitoBean BlogCatalog blog;

  @Test
  void rootServesSpaIndexHtml() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.TEXT_HTML));

    ResponseEntity<String> response =
        rest.exchange("/", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("<div id=\"app\">");
  }

  @Test
  void spaRoutesServeIndexHtml() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.TEXT_HTML));

    ResponseEntity<String> response =
        rest.exchange("/courses/sql", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("<div id=\"app\">");
  }

  @Test
  void blogArticleRouteServesIndexHtml() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.TEXT_HTML));

    ResponseEntity<String> response =
        rest.exchange(
            "/blog/systems/online-code-execution",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("<div id=\"app\">");
  }

  @Test
  void viteHashedAssetsAreNotForwardedToIndexHtml() throws Exception {
    String indexHtml =
        new String(
            java.util.Objects.requireNonNull(
                    getClass().getResourceAsStream("/static/index.html"),
                    "classpath:/static/index.html must exist after SPA copy")
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);

    Matcher js = HASHED_JS.matcher(indexHtml);
    assertThat(js.find()).as("built index.html should reference a hashed JS asset").isTrue();
    String assetPath = js.group(1);

    ResponseEntity<String> asset = rest.getForEntity(assetPath, String.class);

    assertThat(asset.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(asset.getHeaders().getContentType())
        .isNotNull()
        .satisfies(type -> assertThat(type.includes(MediaType.TEXT_HTML)).isFalse());
    assertThat(asset.getBody()).doesNotContain("<div id=\"app\">");
    assertThat(asset.getBody()).isNotBlank();
  }

  @Test
  void apiCoursesRemainsJson() throws Exception {
    when(catalog.list())
        .thenReturn(
            List.of(
                new Course(
                    "sql",
                    "Yet Another SQL Course",
                    "sum",
                    CourseStatus.PUBLISHED,
                    "S",
                    false)));

    mvc.perform(get("/api/courses").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void loginPageIsSpaNotSpringDefault() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.TEXT_HTML));

    ResponseEntity<String> response =
        rest.exchange("/login", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("<div id=\"app\">");
    assertThat(response.getBody()).doesNotContain("Login with OAuth 2.0");
  }

  @Test
  void oauthCallbackPathIsNotForwardedToSpa() throws Exception {
    String body =
        mvc.perform(get("/login/oauth2/code/github").accept(MediaType.TEXT_HTML))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).doesNotContain("<div id=\"app\">");
  }

  @Test
  void githubAuthorizationStartsAtSpringNotSpa() throws Exception {
    mvc.perform(get("/oauth2/authorization/github"))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", startsWith("https://github.com/login/oauth/authorize")));
  }
}
