package io.tayviscon.app.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/** JPA-строка, связывающая OAuth-идентичность с локальным аккаунтом. */
@Entity
@Table(
    name = "auth_identity",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "external_id"}))
public class AuthIdentityEntity {

  @Id private UUID id;

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(nullable = false)
  private String provider;

  @Column(name = "external_id", nullable = false)
  private String externalId;

  protected AuthIdentityEntity() {}

  /** Создаёт строку идентичности для {@code provider}/{@code externalId}. */
  public AuthIdentityEntity(UUID id, UUID accountId, String provider, String externalId) {
    this.id = id;
    this.accountId = accountId;
    this.provider = provider;
    this.externalId = externalId;
  }

  public UUID getId() {
    return id;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public String getProvider() {
    return provider;
  }

  public String getExternalId() {
    return externalId;
  }
}
