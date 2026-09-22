package io.tayviscon.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.tayviscon.core.account.Account;
import io.tayviscon.core.account.InMemoryAccountStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class GitHubOAuthAccountServiceTest {

  @Test
  void githubIntegerIdIsStoredAsDecimalString() {
    InMemoryAccountStore store = new InMemoryAccountStore();
    OAuth2User githubUser =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("SCOPE_read:user")),
            Map.of(
                "id",
                42,
                "login",
                "octocat",
                "name",
                "The Octocat",
                "avatar_url",
                "https://avatars.githubusercontent.com/u/42"),
            "id");
    GitHubOAuthAccountService service =
        new GitHubOAuthAccountService(store, request -> githubUser);

    OAuth2User loaded = service.loadUser(userRequest());

    Account account = store.findOrCreate("github", "42", "ignored", null);
    Object tayvisconId = loaded.getAttribute("tayvisconId");
    assertThat(tayvisconId).isEqualTo(account.id().toString());
    assertThat(Integer.valueOf(store.size())).isEqualTo(1);
    assertThat(loaded.getAuthorities())
        .extracting(authority -> authority.getAuthority())
        .contains("ROLE_USER")
        .doesNotContain("SCOPE_read:user");
  }

  private static OAuth2UserRequest userRequest() {
    ClientRegistration registration =
        ClientRegistration.withRegistrationId("github")
            .clientId("test")
            .clientSecret("test")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationUri("https://github.com/login/oauth/authorize")
            .tokenUri("https://github.com/login/oauth/access_token")
            .userInfoUri("https://api.github.com/user")
            .userNameAttributeName("id")
            .build();
    OAuth2AccessToken token =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "token",
            Instant.parse("2026-09-19T00:00:00Z"),
            Instant.parse("2026-09-19T01:00:00Z"));
    return new OAuth2UserRequest(registration, token);
  }
}
