COMMENT ON TABLE account IS
  'Локальный аккаунт сайта; личность OAuth живёт в auth_identity.';

COMMENT ON COLUMN account.display_name IS
  'Отображаемое имя; обычно с GitHub, можно сменить на сайте.';

COMMENT ON COLUMN account.avatar_url IS
  'URL аватара; NULL — нет картинки или провайдер не отдал.';

COMMENT ON COLUMN account.roles IS
  'Код роли приложения (не CSV). Сейчас: user (по умолчанию).';

COMMENT ON TABLE auth_identity IS
  'Внешняя OAuth-личность, привязанная к account. Одна пара (provider, external_id) — один аккаунт.';

COMMENT ON COLUMN auth_identity.provider IS
  'Идентификатор провайдера, например github. Не человекочитаемое имя.';

COMMENT ON COLUMN auth_identity.external_id IS
  'Стабильный id пользователя у провайдера (для GitHub — numeric user id, не login).';

CREATE INDEX auth_identity_account_id_idx ON auth_identity (account_id);
