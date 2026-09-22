package io.tayviscon.app.account;

import io.tayviscon.core.account.Account;
import io.tayviscon.core.account.AccountStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AccountStore} на PostgreSQL: при конфликте уникальности повторяет поиск.
 */
@Component
public class JpaAccountStore implements AccountStore {

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional
  public Account findOrCreate(
      String provider, String externalId, String displayName, String avatarUrl) {
    return findByIdentity(provider, externalId)
        .map(entity -> syncProfile(entity, displayName, avatarUrl))
        .orElseGet(() -> insertOrRetry(provider, externalId, displayName, avatarUrl));
  }

  private Account insertOrRetry(
      String provider, String externalId, String displayName, String avatarUrl) {
    try {
      var accountId = UUID.randomUUID();
      var account = new AccountEntity(accountId, displayName, avatarUrl, "user");
      em.persist(account);
      em.persist(new AuthIdentityEntity(UUID.randomUUID(), accountId, provider, externalId));
      em.flush();
      return toAccount(account);
    } catch (RuntimeException e) {
      if (!isUniqueConflict(e)) {
        throw e;
      }
      em.clear();
      return findByIdentity(provider, externalId)
          .map(entity -> syncProfile(entity, displayName, avatarUrl))
          .orElseThrow(() -> e);
    }
  }

  private Optional<AccountEntity> findByIdentity(String provider, String externalId) {
    return em.createQuery(
            """
            select a from AccountEntity a, AuthIdentityEntity i
            where i.accountId = a.id and i.provider = :provider and i.externalId = :externalId
            """,
            AccountEntity.class)
        .setParameter("provider", provider)
        .setParameter("externalId", externalId)
        .getResultStream()
        .findFirst();
  }

  private static Account syncProfile(AccountEntity entity, String displayName, String avatarUrl) {
    entity.setDisplayName(displayName);
    entity.setAvatarUrl(avatarUrl);
    return toAccount(entity);
  }

  private static boolean isUniqueConflict(Throwable error) {
    for (Throwable current = error; current != null; current = current.getCause()) {
      if (current instanceof ConstraintViolationException
          || current instanceof DataIntegrityViolationException) {
        return true;
      }
      String message = current.getMessage();
      if (message != null && message.toLowerCase(Locale.ROOT).contains("unique")) {
        return true;
      }
    }
    return false;
  }

  private static Account toAccount(AccountEntity entity) {
    Set<String> roles =
        Arrays.stream(entity.getRoles().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    return new Account(entity.getId(), entity.getDisplayName(), entity.getAvatarUrl(), roles);
  }
}
