package io.tayviscon.app.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

  @Autowired MockMvc mvc;

  @MockitoBean CourseCatalog catalog;

  @MockitoBean BlogCatalog blogCatalog;

  @Test
  void anonymousCanGetCoursesMeAndBlog() throws Exception {
    when(blogCatalog.tree()).thenReturn(List.of());
    mvc.perform(get("/api/courses")).andExpect(status().isOk());
    mvc.perform(get("/api/me")).andExpect(status().isOk());
    mvc.perform(get("/api/blog/tree")).andExpect(status().isOk());
  }

  @Test
  void anonymousUnknownApiReturnsUnauthorizedJson() throws Exception {
    mvc.perform(get("/api/nope").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  void getLogoutDoesNotTreatNavigationAsLogout() throws Exception {
    var result =
        mvc.perform(get("/logout").with(oauth2Login().oauth2User(signedInUser())))
            .andExpect(status().isOk())
            .andReturn();
    MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
    mvc.perform(get("/api/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true));
  }

  @Test
  void postLogoutWithoutCsrfIsForbidden() throws Exception {
    mvc.perform(post("/logout").with(oauth2Login())).andExpect(status().isForbidden());
  }

  @Test
  void postLogoutRedirectsHome() throws Exception {
    mvc.perform(post("/logout").with(oauth2Login()).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  private static OAuth2User signedInUser() {
    UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    return new DefaultOAuth2User(
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
  }
}
