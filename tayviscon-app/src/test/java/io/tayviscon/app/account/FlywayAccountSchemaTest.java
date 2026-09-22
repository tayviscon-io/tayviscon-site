package io.tayviscon.app.account;

import static org.assertj.core.api.Assertions.assertThat;

import io.tayviscon.core.course.CourseCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class FlywayAccountSchemaTest {

  @Autowired JdbcTemplate jdbc;

  @MockitoBean CourseCatalog catalog;

  @Test
  void accountAndIdentityHaveCatalogComments() {
    assertThat(tableRemarks("account"))
        .isEqualTo("Локальный аккаунт сайта; личность OAuth живёт в auth_identity.");
    assertThat(columnRemarks("account", "roles"))
        .isEqualTo("Код роли приложения (не CSV). Сейчас: user (по умолчанию).");
    assertThat(tableRemarks("auth_identity"))
        .contains("Одна пара (provider, external_id) — один аккаунт.");
    assertThat(columnRemarks("auth_identity", "external_id")).contains("numeric user id");
  }

  @Test
  void authIdentityAccountIdIsIndexed() {
    Integer indexes =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.INDEX_COLUMNS
            WHERE LOWER(TABLE_NAME) = 'auth_identity'
              AND LOWER(COLUMN_NAME) = 'account_id'
            """,
            Integer.class);
    assertThat(indexes).isGreaterThanOrEqualTo(1);
  }

  private String tableRemarks(String table) {
    return jdbc.queryForObject(
        """
        SELECT REMARKS
        FROM INFORMATION_SCHEMA.TABLES
        WHERE LOWER(TABLE_NAME) = ?
        """,
        String.class,
        table);
  }

  private String columnRemarks(String table, String column) {
    return jdbc.queryForObject(
        """
        SELECT REMARKS
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE LOWER(TABLE_NAME) = ? AND LOWER(COLUMN_NAME) = ?
        """,
        String.class,
        table,
        column);
  }
}
