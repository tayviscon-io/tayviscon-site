package io.tayviscon.app.me;

import com.fasterxml.jackson.annotation.JsonInclude;

/** JSON-представление посетителя для {@code /api/me}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MeResponse(boolean authenticated, String id, String name, String avatarUrl) {

  /** Гостевой ответ, когда нет OAuth-сессии. */
  public static MeResponse guest() {
    return new MeResponse(false, null, null, null);
  }

  /** Ответ для вошедшего пользователя с id аккаунта и полями профиля GitHub. */
  public static MeResponse authenticated(String id, String name, String avatarUrl) {
    return new MeResponse(true, id, name, avatarUrl);
  }
}
