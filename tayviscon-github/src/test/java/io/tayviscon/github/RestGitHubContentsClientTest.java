package io.tayviscon.github;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestGitHubContentsClientTest {
  private RestClient.Builder restClientBuilder;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    restClientBuilder = RestClient.builder().baseUrl("https://api.github.com");
    server = MockRestServiceServer.bindTo(restClientBuilder).build();
  }

  @AfterEach
  void verifyServer() {
    server.verify();
  }

  private RestGitHubContentsClient client(Optional<String> token) {
    return new RestGitHubContentsClient(restClientBuilder, token);
  }

  private RestGitHubContentsClient blogClient() {
    return new RestGitHubContentsClient(
        restClientBuilder, Optional.empty(), "tayviscon-io", "tayviscon-knowledge-base", "main");
  }

  @Test
  void getFileReturnsBodyOn200() {
    server
        .expect(
            requestTo(
                "https://api.github.com/repos/tayviscon-io/yet-another-course/contents/catalog.yaml?ref=main"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Accept", "application/vnd.github.raw"))
        .andExpect(headerDoesNotExist("Authorization"))
        .andRespond(withSuccess("catalog content", MediaType.TEXT_PLAIN));

    assertEquals("catalog content", client(Optional.empty()).getFile("catalog.yaml"));
  }

  @Test
  void getRawReturnsBytesOn200() {
    var bytes = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
    server
        .expect(
            requestTo(
                "https://api.github.com/repos/tayviscon-io/yet-another-course/contents/courses/sql/logo.png?ref=main"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Accept", "application/vnd.github.raw"))
        .andRespond(withSuccess(bytes, MediaType.APPLICATION_OCTET_STREAM));

    assertArrayEquals(bytes, client(Optional.empty()).getRaw("courses/sql/logo.png"));
  }

  @Test
  void sendsBearerTokenWhenPresent() {
    server
        .expect(
            requestTo(
                "https://api.github.com/repos/tayviscon-io/yet-another-course/contents/catalog.yaml?ref=main"))
        .andExpect(header("Authorization", "Bearer ghp_test_token"))
        .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

    client(Optional.of("ghp_test_token")).getFile("catalog.yaml");
  }

  @Test
  void timedRequestFactorySetsConnectAndReadTimeouts() {
    var factory = RestGitHubContentsClient.timedRequestFactory();
    assertEquals(
        (int) Duration.ofSeconds(5).toMillis(),
        ReflectionTestUtils.getField(factory, "connectTimeout"));
    assertEquals(
        (int) Duration.ofSeconds(10).toMillis(),
        ReflectionTestUtils.getField(factory, "readTimeout"));
  }

  @Test
  void throwsOn500() {
    server
        .expect(requestTo(containsString("/contents/catalog.yaml")))
        .andRespond(withServerError());

    var thrown =
        assertThrows(
            RuntimeException.class, () -> client(Optional.empty()).getFile("catalog.yaml"));
    assertTrue(thrown.getMessage().contains("HTTP 500"));
  }

  @Test
  void throwsOn404() {
    server
        .expect(requestTo(containsString("/contents/missing.yaml")))
        .andRespond(withResourceNotFound());

    var thrown =
        assertThrows(
            RuntimeException.class, () -> client(Optional.empty()).getFile("missing.yaml"));
    assertTrue(thrown.getMessage().contains("HTTP 404"));
  }

  @Test
  void throwsOn403() {
    server
        .expect(requestTo(containsString("/contents/secret.bin")))
        .andRespond(withForbiddenRequest());

    var thrown =
        assertThrows(RuntimeException.class, () -> client(Optional.empty()).getRaw("secret.bin"));
    assertTrue(thrown.getMessage().contains("HTTP 403"));
  }

  @Test
  void getFileUsesUtf8() {
    var cyrillic = "содержимое каталога";
    server
        .expect(requestTo(containsString("/contents/catalog.yaml")))
        .andRespond(
            withSuccess(cyrillic.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN));

    assertEquals(cyrillic, client(Optional.empty()).getFile("catalog.yaml"));
  }

  @Test
  void getFileUsesConfiguredRepo() {
    server
        .expect(
            requestTo(
                "https://api.github.com/repos/tayviscon-io/tayviscon-knowledge-base/contents/systems/online-code-execution/index.md?ref=main"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("# hi", MediaType.TEXT_PLAIN));
    assertEquals("# hi", blogClient().getFile("systems/online-code-execution/index.md"));
  }

  @Test
  void listTreeReturnsRecursiveBlobs() {
    String body =
        """
        {"tree":[
          {"path":"systems","type":"tree"},
          {"path":"systems/online-code-execution/index.md","type":"blob"},
          {"path":"systems/online-code-execution/engine.excalidraw.svg","type":"blob"}
        ]}
        """;
    server
        .expect(
            requestTo(
                "https://api.github.com/repos/tayviscon-io/tayviscon-knowledge-base/git/trees/main?recursive=1"))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    var tree = blogClient().listTree();
    assertEquals(3, tree.size());
    assertTrue(tree.get(0).directory());
    assertEquals("systems/online-code-execution/index.md", tree.get(1).path());
    assertFalse(tree.get(1).directory());
  }

  @Test
  void rejectsParentPathSegments() {
    assertThrows(
        IllegalArgumentException.class,
        () -> client(Optional.empty()).getFile("../README.md"));
    assertThrows(
        IllegalArgumentException.class,
        () -> client(Optional.empty()).getRaw("/etc/passwd"));
  }
}
