package io.tayviscon.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApplicationSecretsTest {

  @Test
  void mainYamlKeepsDatasourceAndOauthSecretsInEnvPlaceholders() throws Exception {
    String yaml = Files.readString(Path.of("src/main/resources/application.yml"));

    assertThat(yaml).doesNotContain("password: tayviscon");
    assertThat(yaml).contains("password: ${SPRING_DATASOURCE_PASSWORD}");
    assertThat(yaml).doesNotContain("GITHUB_OAUTH_CLIENT_SECRET:placeholder");
    assertThat(yaml).contains("client-secret: ${GITHUB_OAUTH_CLIENT_SECRET:}");
  }
}
