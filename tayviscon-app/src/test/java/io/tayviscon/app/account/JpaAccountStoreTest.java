package io.tayviscon.app.account;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAccountStore.class)
class JpaAccountStoreTest {

  @Autowired JpaAccountStore store;

  @Test
  void sameGithubIdIsSameAccount() {
    var a = store.findOrCreate("github", "42", "Tyomych", "http://a");
    var b = store.findOrCreate("github", "42", "Other", "http://b");
    assertEquals(a.id(), b.id());
  }

  @Test
  void updatesNameAndAvatarForExistingGithubIdentity() {
    store.findOrCreate("github", "42", "Tyomych", "http://a");
    var updated = store.findOrCreate("github", "42", "Other", "http://b");
    assertEquals("Other", updated.displayName());
    assertEquals("http://b", updated.avatarUrl());
  }
}
