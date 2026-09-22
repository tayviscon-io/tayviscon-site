package io.tayviscon.app.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** JPA-строка локального аккаунта сайта. */
@Entity
@Table(name = "account")
public class AccountEntity {

  @Id private UUID id;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(nullable = false)
  private String roles = "user";

  protected AccountEntity() {}

  /** Создаёт строку аккаунта с идентификаторами и полями профиля. */
  public AccountEntity(UUID id, String displayName, String avatarUrl, String roles) {
    this.id = id;
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
    this.roles = roles;
  }

  public UUID getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public String getRoles() {
    return roles;
  }
}
