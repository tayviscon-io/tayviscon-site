package io.tayviscon.core.account;

/** Ищет или создаёт локальный аккаунт для внешней OAuth-идентичности. */
public interface AccountStore {
  /**
   * Возвращает аккаунт для {@code provider}/{@code externalId}, создавая его при необходимости.
   */
  Account findOrCreate(
      String provider, String externalId, String displayName, String avatarUrl);
}
