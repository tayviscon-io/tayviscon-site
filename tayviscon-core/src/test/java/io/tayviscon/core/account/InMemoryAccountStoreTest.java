package io.tayviscon.core.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class InMemoryAccountStoreTest {
  @Test
  void sameGithubIdIsSameAccount() {
    var store = new InMemoryAccountStore();
    var a = store.findOrCreate("github", "42", "Tyomych", "http://a");
    var b = store.findOrCreate("github", "42", "Other", "http://b");
    assertEquals(a.id(), b.id());
    assertEquals(1, store.size());
  }

  @Test
  void updatesNameAndAvatarForExistingGithubIdentity() {
    var store = new InMemoryAccountStore();
    store.findOrCreate("github", "42", "Tyomych", "http://a");
    var updated = store.findOrCreate("github", "42", "Other", "http://b");
    assertEquals("Other", updated.displayName());
    assertEquals("http://b", updated.avatarUrl());
  }

  @Test
  void differentProviderDoesNotCollide() {
    var store = new InMemoryAccountStore();
    var a = store.findOrCreate("github", "42", "A", null);
    var b = store.findOrCreate("gitlab", "42", "B", null);
    assertNotEquals(a.id(), b.id());
  }
}
