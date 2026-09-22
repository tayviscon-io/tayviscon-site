package io.tayviscon.app;

import io.tayviscon.core.blog.BlogCatalog;
import io.tayviscon.core.blog.BlogUnavailableException;
import io.tayviscon.core.course.CatalogUnavailableException;
import io.tayviscon.core.course.CourseCatalog;
import io.tayviscon.github.GitHubBlogCatalog;
import io.tayviscon.github.GitHubContentsClient;
import io.tayviscon.github.GitHubCourseCatalog;
import io.tayviscon.github.RestGitHubContentsClient;
import io.tayviscon.github.SvgLogoCleaner;
import io.tayviscon.github.YamlCatalogParser;
import io.tayviscon.renderer.MarkdownRenderer;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Собирает каталог курсов и блог с GitHub и прогревает их при старте. */
@Configuration
public class GitHubCatalogConfig {
  private static final Logger log = LoggerFactory.getLogger(GitHubCatalogConfig.class);

  /** Клиент Contents API для репозитория курсов. */
  @Bean
  @Qualifier("courseContents")
  GitHubContentsClient courseContents() {
    return contentsClient("tayviscon-io", "yet-another-course", "main");
  }

  /** Клиент Contents API для knowledge-base (блог). */
  @Bean
  @Qualifier("blogContents")
  GitHubContentsClient blogContents() {
    return contentsClient(
        envOr("BLOG_GITHUB_OWNER", "tayviscon-io"),
        envOr("BLOG_GITHUB_REPO", "tayviscon-knowledge-base"),
        envOr("BLOG_GITHUB_REF", "main"));
  }

  /** Каталог курсов поверх {@code courseContents}. */
  @Bean
  CourseCatalog courseCatalog(@Qualifier("courseContents") GitHubContentsClient client) {
    return new GitHubCourseCatalog(client, new YamlCatalogParser(), new SvgLogoCleaner());
  }

  /** Каталог блога поверх {@code blogContents}. */
  @Bean
  BlogCatalog blogCatalog(@Qualifier("blogContents") GitHubContentsClient client) {
    return new GitHubBlogCatalog(client, new MarkdownRenderer());
  }

  /** Прогревает курсы и блог при старте, не роняя приложение при сбое GitHub. */
  @Bean
  ApplicationListener<ApplicationReadyEvent> catalogWarmup(
      CourseCatalog catalog, BlogCatalog blog) {
    return event -> {
      try {
        catalog.list();
      } catch (CatalogUnavailableException e) {
        log.warn("GitHub catalog warmup failed; serving requests when GitHub recovers", e);
      }
      try {
        blog.tree();
      } catch (BlogUnavailableException e) {
        log.warn("GitHub blog warmup failed; serving requests when GitHub recovers", e);
      }
    };
  }

  private static GitHubContentsClient contentsClient(String owner, String repo, String ref) {
    String token = System.getenv("GITHUB_TOKEN");
    Optional<String> tokenOpt =
        token == null || token.isBlank() ? Optional.empty() : Optional.of(token);
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(10));
    return new RestGitHubContentsClient(
        RestClient.builder().baseUrl("https://api.github.com").requestFactory(factory),
        tokenOpt,
        owner,
        repo,
        ref);
  }

  private static String envOr(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }
}
