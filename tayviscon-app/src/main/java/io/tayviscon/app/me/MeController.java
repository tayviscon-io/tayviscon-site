package io.tayviscon.app.me;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-эндпоинт текущего посетителя или гостевой ответ. */
@RestController
public class MeController {

  /** Возвращает текущего пользователя или неаутентифицированного гостя. */
  @GetMapping("/api/me")
  public MeResponse me(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User user)) {
      return MeResponse.guest();
    }
    Object tayvisconId = user.getAttribute("tayvisconId");
    if (tayvisconId == null) {
      return MeResponse.guest();
    }
    String name = text(user, "name");
    if (name == null || name.isBlank()) {
      name = text(user, "login");
    }
    return MeResponse.authenticated(tayvisconId.toString(), name, text(user, "avatar_url"));
  }

  private static String text(OAuth2User user, String key) {
    Object value = user.getAttribute(key);
    return value == null ? null : String.valueOf(value);
  }
}
