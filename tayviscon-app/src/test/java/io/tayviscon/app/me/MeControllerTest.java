package io.tayviscon.app.me;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.tayviscon.core.blog.BlogCatalog;
import io.tayviscon.core.course.CourseCatalog;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MeControllerTest {

  @Autowired MockMvc mvc;

  @MockitoBean CourseCatalog catalog;

  @MockitoBean BlogCatalog blog;

  @Test
  void guest() throws Exception {
    mvc.perform(get("/api/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false));
  }

  @Test
  void authenticatedReadsTayvisconIdFromOAuth2User() throws Exception {
    UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    OAuth2User user =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of(
                "id",
                42,
                "login",
                "octocat",
                "name",
                "The Octocat",
                "avatar_url",
                "https://avatars.githubusercontent.com/u/42",
                "tayvisconId",
                accountId.toString()),
            "id");

    mvc.perform(get("/api/me").with(oauth2Login().oauth2User(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.id").value(accountId.toString()))
        .andExpect(jsonPath("$.name").value("The Octocat"))
        .andExpect(jsonPath("$.avatarUrl").value("https://avatars.githubusercontent.com/u/42"));
  }
}
