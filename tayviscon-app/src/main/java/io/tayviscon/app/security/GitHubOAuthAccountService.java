package io.tayviscon.app.security;

import io.tayviscon.core.account.Account;
import io.tayviscon.core.account.AccountStore;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/** Загружает пользователей GitHub OAuth и создаёт локальный аккаунт с ролями. */
@Service
public class GitHubOAuthAccountService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final AccountStore accountStore;
  private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

  /**
   * Создаёт сервис, который запрашивает профиль GitHub через клиент Spring по умолчанию.
   */
  @Autowired
  public GitHubOAuthAccountService(AccountStore accountStore) {
    this(accountStore, new DefaultOAuth2UserService());
  }

  GitHubOAuthAccountService(
      AccountStore accountStore, OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate) {
    this.accountStore = accountStore;
    this.delegate = delegate;
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oauth2User = delegate.loadUser(userRequest);
    String id = text(oauth2User, "id");
    String name = text(oauth2User, "name");
    if (name == null || name.isBlank()) {
      name = text(oauth2User, "login");
    }
    String avatar = text(oauth2User, "avatar_url");
    Account account = accountStore.findOrCreate("github", id, name, avatar);
    Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
    attributes.put("tayvisconId", account.id().toString());
    String nameAttributeKey =
        userRequest
            .getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();
    var authorities =
        account.roles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
            .toList();
    return new DefaultOAuth2User(authorities, attributes, nameAttributeKey);
  }

  private static String text(OAuth2User user, String key) {
    Object value = user.getAttribute(key);
    return value == null ? null : String.valueOf(value);
  }
}
