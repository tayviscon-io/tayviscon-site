# Tayviscon IO site

Витрина организации Tayviscon IO: главная, каталог курсов из [Yet Another Course](https://github.com/tayviscon-io/yet-another-course), блог из [tayviscon-knowledge-base](https://github.com/tayviscon-io/tayviscon-knowledge-base), вход через GitHub.

## Требования

- Java 21
- Maven 3.9+
- Docker (PostgreSQL)

## Локальный запуск

### 1. PostgreSQL

```powershell
docker compose up -d
```

База: `tayviscon`, пользователь и пароль `tayviscon`, порт `5432` только на `127.0.0.1`. Этот пароль — локальный default Docker, не для production.

### 2. Переменные окружения

| Переменная | Обязательна | Описание |
|---|---|---|
| `SPRING_DATASOURCE_PASSWORD` | да | Пароль Postgres. Локально совпадает с Compose: `tayviscon` |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | нет | `true` на HTTPS/VPS, иначе браузер не отправит session cookie. YAML default — `false` (localhost HTTP). |
| `GITHUB_OAUTH_CLIENT_ID` | да (для входа) | Client ID GitHub OAuth App |
| `GITHUB_OAUTH_CLIENT_SECRET` | да (для входа) | Client Secret GitHub OAuth App |
| `GITHUB_TOKEN` | нет | Fine-grained PAT `Contents: Read` на `yet-another-course` и `tayviscon-knowledge-base`; снижает риск rate limit |
| `BLOG_GITHUB_OWNER` | нет | Owner репо блога (default `tayviscon-io`) |
| `BLOG_GITHUB_REPO` | нет | Репо блога (default `tayviscon-knowledge-base`) |
| `BLOG_GITHUB_REF` | нет | Ветка блога (default `main`) |

Пример (PowerShell):

```powershell
$env:SPRING_DATASOURCE_PASSWORD = "tayviscon"
$env:GITHUB_OAUTH_CLIENT_ID = "your-client-id"
$env:GITHUB_OAUTH_CLIENT_SECRET = "your-client-secret"
# опционально:
$env:GITHUB_TOKEN = "ghp_..."
# production HTTPS/VPS only (не для localhost HTTP):
# $env:SERVER_SERVLET_SESSION_COOKIE_SECURE = "true"
```

Шаблон переменных: `.env.example` (не коммитить заполненный `.env`).

**GitHub OAuth App:** создайте приложение в [GitHub Developer settings](https://github.com/settings/developers). Callback URL:

```
http://localhost:8080/login/oauth2/code/github
```

Не коммитьте секреты в репозиторий.

### 3. Spring Boot

Из корня репозитория:

```powershell
mvn -pl tayviscon-app -am spring-boot:run
```

Флаг `-am` собирает зависимые модули (`tayviscon-core`, `tayviscon-github`, `tayviscon-renderer`).

### 4. Открыть сайт

```
http://localhost:8080
```

Каталог курсов доступен без входа. Кнопка «Войти» открывает `/login`; оттуда «Продолжить с GitHub» ведёт на GitHub OAuth.

## Сборка и тесты

```powershell
mvn verify
```

`mvn verify` гоняет Checkstyle, Surefire, `npm run lint` и Vitest (`frontend-maven-plugin` в `tayviscon-ui`).

## Продакшен

Сайт едет на Cloud VPS REG.RU (шаблон Docker) через Compose и GitHub Actions. Что такое Docker, Caddyfile, Temurin и Actions, и какие шаги ручные: [`docs/deploy.md`](docs/deploy.md).

## Модули

| Модуль | Назначение |
|---|---|
| `tayviscon-core` | домен: курсы, аккаунты |
| `tayviscon-github` | GitHub Contents API, YAML, кэш |
| `tayviscon-renderer` | Markdown → HTML (v1 не используется API) |
| `tayviscon-ui` | Vue 3 SPA |
| `tayviscon-app` | Spring Boot, REST, OAuth, статика |
