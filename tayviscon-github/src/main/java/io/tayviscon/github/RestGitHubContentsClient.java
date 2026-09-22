package io.tayviscon.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Клиент GitHub Contents/Trees с owner/repo/ref, проверкой путей, таймаутами и необязательным
 * bearer-токеном.
 */
public class RestGitHubContentsClient implements GitHubContentsClient {
  private static final String BASE_URL = "https://api.github.com";
  private static final String ACCEPT_RAW = "application/vnd.github.raw";
  private static final String ACCEPT_JSON = "application/vnd.github+json";
  static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

  private final RestClient restClient;
  private final Optional<String> token;
  private final String owner;
  private final String repo;
  private final String ref;

  /** Создаёт клиент к указанному репозиторию и ветке. */
  public RestGitHubContentsClient(
      RestClient.Builder restClientBuilder,
      Optional<String> token,
      String owner,
      String repo,
      String ref) {
    this.restClient = restClientBuilder.build();
    this.token = token;
    this.owner = owner;
    this.repo = repo;
    this.ref = ref;
  }

  /** Создаёт клиент к yet-another-course на {@code main}. */
  public RestGitHubContentsClient(Optional<String> token) {
    this(
        RestClient.builder().baseUrl(BASE_URL).requestFactory(timedRequestFactory()),
        token,
        "tayviscon-io",
        "yet-another-course",
        "main");
  }

  /** Создаёт клиент к yet-another-course на {@code main} с заданным {@link RestClient.Builder}. */
  public RestGitHubContentsClient(RestClient.Builder restClientBuilder, Optional<String> token) {
    this(restClientBuilder, token, "tayviscon-io", "yet-another-course", "main");
  }

  static SimpleClientHttpRequestFactory timedRequestFactory() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT);
    factory.setReadTimeout(READ_TIMEOUT);
    return factory;
  }

  @Override
  public String getFile(String path) {
    return new String(fetchBytes(path), StandardCharsets.UTF_8);
  }

  @Override
  public byte[] getRaw(String path) {
    return fetchBytes(path);
  }

  @Override
  public List<GitHubTreeEntry> listTree() {
    var request =
        restClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/repos/" + owner + "/" + repo + "/git/trees/" + ref)
                        .queryParam("recursive", 1)
                        .build())
            .header("Accept", ACCEPT_JSON);
    if (token.isPresent()) {
      request = request.header("Authorization", "Bearer " + token.get());
    }
    String body =
        request
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (req, res) -> {
                  throw new RuntimeException(
                      "GitHub API error: HTTP "
                          + res.getStatusCode().value()
                          + " for git/trees/"
                          + ref);
                })
            .body(String.class);
    return parseTree(body);
  }

  static void requireSafePath(String path) {
    if (path == null || path.isBlank() || path.startsWith("/") || path.contains("\\")) {
      throw new IllegalArgumentException("Unsafe GitHub contents path");
    }
    for (String segment : path.split("/")) {
      if (segment.isBlank() || "..".equals(segment) || ".".equals(segment)) {
        throw new IllegalArgumentException("Unsafe GitHub contents path");
      }
    }
  }

  private byte[] fetchBytes(String path) {
    requireSafePath(path);
    var request =
        restClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/repos/" + owner + "/" + repo + "/contents/")
                        .path(path)
                        .queryParam("ref", ref)
                        .build())
            .header("Accept", ACCEPT_RAW);
    if (token.isPresent()) {
      request = request.header("Authorization", "Bearer " + token.get());
    }
    return request
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {
          throw new RuntimeException(
              "GitHub API error: HTTP " + res.getStatusCode().value() + " for " + path);
        })
        .body(byte[].class);
  }

  private static List<GitHubTreeEntry> parseTree(String body) {
    try {
      JsonNode tree = new ObjectMapper().readTree(body == null ? "{}" : body).get("tree");
      if (tree == null || !tree.isArray()) {
        return List.of();
      }
      List<GitHubTreeEntry> entries = new ArrayList<>();
      for (JsonNode node : tree) {
        JsonNode pathNode = node.get("path");
        if (pathNode == null || pathNode.isNull()) {
          continue;
        }
        boolean directory = "tree".equals(node.path("type").asText());
        entries.add(new GitHubTreeEntry(pathNode.asText(), directory));
      }
      return entries;
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse GitHub tree", e);
    }
  }
}
