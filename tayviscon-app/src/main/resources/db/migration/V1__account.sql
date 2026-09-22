CREATE TABLE account (
  id UUID PRIMARY KEY,
  display_name VARCHAR(255) NOT NULL,
  avatar_url VARCHAR(512),
  roles VARCHAR(64) NOT NULL DEFAULT 'user'
);

CREATE TABLE auth_identity (
  id UUID PRIMARY KEY,
  account_id UUID NOT NULL REFERENCES account(id),
  provider VARCHAR(32) NOT NULL,
  external_id VARCHAR(128) NOT NULL,
  UNIQUE (provider, external_id)
);
