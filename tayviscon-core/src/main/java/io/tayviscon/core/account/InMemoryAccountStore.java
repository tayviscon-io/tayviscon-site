package io.tayviscon.core.account;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Непотокобезопасное хранилище {@link AccountStore} в памяти для тестов и локальной работы. */
public final class InMemoryAccountStore implements AccountStore {
  private record IdentityKey(String provider, String externalId) {}

  private final Map<IdentityKey, Account> byIdentity = new HashMap<>();

  @Override
  public Account findOrCreate(
      String provider, String externalId, String displayName, String avatarUrl) {
    var key = new IdentityKey(provider, externalId);
    var existing = byIdentity.get(key);
    if (existing != null) {
      var updated = new Account(existing.id(), displayName, avatarUrl, existing.roles());
      byIdentity.put(key, updated);
      return updated;
    }
    var account = new Account(UUID.randomUUID(), displayName, avatarUrl, Set.of("user"));
    byIdentity.put(key, account);
    return account;
  }

  /** Возвращает число сохранённых идентичностей. */
  public int size() {
    return byIdentity.size();
  }
}
