package io.tayviscon.app.account;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class JpaAccountStoreConflictTest {

  @Test
  void uniqueConstraintOnInsertRetriesSelect() throws Exception {
    AccountEntity existing =
        new AccountEntity(UUID.randomUUID(), "Kept", "http://a", "user");
    AtomicInteger lookups = new AtomicInteger();
    EntityManager em =
        (EntityManager)
            Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(),
                new Class<?>[] {EntityManager.class},
                (proxy, method, args) -> {
                  return switch (method.getName()) {
                    case "createQuery" -> query(lookups.getAndIncrement() == 0, existing);
                    case "persist" -> persist(args[0]);
                    case "flush", "clear" -> null;
                    default ->
                        throw new UnsupportedOperationException(method.getName());
                  };
                });

    JpaAccountStore store = new JpaAccountStore();
    var field = JpaAccountStore.class.getDeclaredField("em");
    field.setAccessible(true);
    field.set(store, em);

    var account = store.findOrCreate("github", "7", "Kept", "http://a");

    assertEquals(existing.getId(), account.id());
    assertEquals(2, lookups.get());
  }

  private static Object persist(Object entity) {
    if (entity instanceof AuthIdentityEntity) {
      throw new PersistenceException("unique constraint");
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static TypedQuery<AccountEntity> query(boolean empty, AccountEntity existing) {
    return (TypedQuery<AccountEntity>)
        Proxy.newProxyInstance(
            TypedQuery.class.getClassLoader(),
            new Class<?>[] {TypedQuery.class},
            (proxy, method, args) ->
                switch (method.getName()) {
                  case "setParameter" -> proxy;
                  case "getResultStream" -> empty ? Stream.empty() : Stream.of(existing);
                  default -> throw new UnsupportedOperationException(method.getName());
                });
  }
}
