# Деплой сайта Tayviscon

Этот документ — и буквальный разбор терминов, и пошаговая инструкция. Если слова вроде Docker, Caddyfile, Temurin и GitHub Actions ничего не значат, начните с раздела «Что происходит». Практические шаги — ниже, в «Ручные шаги» и «Что уже в git».

## Что мы хотим получить

Посетитель вводит в браузере адрес вроде `https://tayviscon.io`. Браузер должен получить HTML витрины, каталог курсов, вход через GitHub.

Для этого на компьютере в интернете должны крутиться три процесса:

1. **Сайт** — наша программа на Java (Spring Boot). Внутри одного файла-архива (fat JAR) уже лежит собранный Vue. Отдельный сервер «только для фронтенда» не нужен.
2. **База данных** — PostgreSQL 16. В ней аккаунты и привязки GitHub. Схему накатывает Flyway при старте сайта.
3. **Вход с улицы** — программа, которая принимает HTTPS (порт 443), держит сертификат Let’s Encrypt и передаёт запрос сайту. Сайт сам слушает только внутренний порт 8080, с интернета до него достучаться нельзя.

Этот компьютер мы не ставим дома, а арендуем у REG.RU: **Cloud VPS** (виртуальный сервер) с уже установленным Docker.

Классический «хостинг сайтов» REG.RU (PHP, MySQL, папка `public_html`) **не подходит**. Там нет Java, нет долгоживущего процесса Spring Boot и нет PostgreSQL 16 как у нас в проекте.

---

## Что происходит: словарь

### VPS

Виртуальный сервер: удалённый Linux (Ubuntu 24.04), к которому вы заходите по SSH. На нём можно ставить программы как на свой компьютер. Мы просим у REG.RU шаблон **Docker**, чтобы не устанавливать Docker руками: [reg.cloud/apps/docker](https://reg.cloud/apps/docker).

### Docker

Docker запускает программу в **контейнере**: изолированный процесс со своим набором файлов, но на ядре той же Ubuntu.

Три связанных понятия:

| Слово | Что это |
|---|---|
| **Dockerfile** | Текстовый рецепт: из какого готового Linux+Java начать, какие файлы положить, какой командой запускать сайт. У нас это файл `Dockerfile` в корне репозитория. |
| **Образ (image)** | Результат сборки рецепта. Его можно сохранить в реестре и скачать на VPS. Один образ → много одинаковых запусков. |
| **Контейнер** | Уже запущенный процесс из образа. «Поднять контейнер» = стартовать сайт / Postgres / Caddy. |

Зачем не поставить Java и Postgres пакетами `apt` на сам VPS (как в старом плане с systemd):

- на ноутбуке и на сервере получаются **разные** версии Java и Postgres;
- откат = «положи старый JAR обратно и молись, что JRE та же»;
- сборка `mvn` на сервере тянет Node, тесты и интернет в прод.

С Docker версия Java зашита в образ (Temurin 21), Postgres — официальный образ `postgres:16`, как локально. Откат = запустить предыдущий тег образа.

### Temurin

**Eclipse Temurin** — готовая сборка Java от Eclipse Adoptium. Это не отдельный язык и не фреймворк. Это конкретный **JRE 21**: среда, в которой выполняется наш JAR.

В `Dockerfile` строка `FROM eclipse-temurin:21-jre-jammy` значит: «возьми Ubuntu Jammy, где уже лежит Java 21». Проект собран под Java 21 (`pom.xml`), поэтому 17 или 24 не берём.

Почему Temurin, а не «просто java с Ubuntu» и не Liberica/GraalVM:

- одна и та же сборка в документации Spring, в GitHub Actions (`distribution: temurin`) и в образе;
- поддержка LTS 21;
- Liberica в примерах Spring часто для CDS/AOT — нам это в первом деплое не нужно (тренировочный прогон без живой БД ломается).

### Fat JAR и слои

Maven собирает **один** файл `tayviscon-app-0.1.0-SNAPSHOT.jar`: туда упакованы Java-код, зависимости и статика Vue. Его можно запустить `java -jar ...`.

`Dockerfile` не копирует этот JAR как есть. Команда `-Djarmode=tools extract --layers` разрезает его на слои (зависимости отдельно, наш код отдельно). Docker тогда при обновлении сайта чаще качает только тонкий слой с нашим кодом, а не все библиотеки заново. Это рекомендация Spring Boot 3.4, не «магия Docker».

### Caddy и Caddyfile («кодифай»)

**Caddy** — веб-сервер. Он стоит **перед** сайтом и делает две вещи, которых Spring Boot в проде сам не должен делать:

1. Слушает порты **80 и 443** (то, что открыто в интернет).
2. Сам получает и продлевает HTTPS-сертификат Let’s Encrypt, когда DNS уже указывает на VPS.

**Caddyfile** — конфиг Caddy. У нас это `deploy/Caddyfile`. По смыслу это не «кодифай» как отдельный продукт, а файл с именем Caddyfile: «для домена `$SITE_HOST` отдай всё приложению `app` на порту 8080».

Почему Caddy, а не nginx:

- nginx тоже умеет прокси, но сертификат обычно вешают через certbot и cron;
- Caddy делает HTTPS из коробки, для одного сайта на одном VPS меньше движущихся частей;
- Traefik рассчитан на динамическую россыпь сервисов — у нас три контейнера.

Caddy **не заменяет** Spring Boot. Браузер → Caddy → контейнер `app`.

### Docker Compose

Compose читает YAML и поднимает **несколько** контейнеров вместе, с общей сетью.

Два разных файла специально, чтобы не перепутать ноутбук и прод:

| Файл | Где | Что внутри |
|---|---|---|
| `docker-compose.yml` | ноутбук | только Postgres на `127.0.0.1:5432`, пароль `tayviscon`. Сайт вы запускаете Maven’ом. |
| `compose.prod.yml` | VPS | Caddy + сайт + Postgres. Наружу только 80/443. Пароль БД **обязателен и другой**. |

Контейнеры в прод-файле обращаются друг к другу по имени сервиса: сайт стучится в `postgres:5432`, Caddy — в `app:8080`. Это имена в внутренней сети Docker, не публичные DNS.

### GitHub Actions

Это не хостинг сайта. Это **робот в облаке GitHub**, который по событию в репозитории запускает скрипт из `.github/workflows/ci.yml`.

У нас три работы:

1. **verify** (каждый PR и каждый пуш в `main`) — на чистой Ubuntu ставится Java 21 Temurin, выполняется `mvn -B verify`: Checkstyle, тесты Java, ESLint, Vitest, сборка Vue. Секреты не нужны. Если verify красный, образ и деплой не идут.
2. **image** (только `main`) — уже собранный JAR превращается в Docker-образ и отправляется в реестр.
3. **deploy** (только `main`) — по SSH на VPS копируются `compose.prod.yml` и Caddyfile, затем `docker compose pull && up -d`.

Робот **не** получает пароль базы и OAuth. Они лежат только в `/opt/tayviscon/.env` на сервере. В GitHub environment `production` — только ключ SSH, чтобы робот мог зайти на VPS.

### GHCR (GitHub Container Registry)

Реестр образов по адресу `ghcr.io`, рядом с репозиторием. Образ называется так:

`ghcr.io/tayviscon-io/tayviscon-site:<git-sha>`

и ещё ярлык `:main` на последний успешный `main`.

Почему не Docker Hub: лишний аккаунт и лимиты. Почему не «просто скопировать JAR по SCP»: откат и повторная установка VPS тогда хуже, JRE снова «какая получится на машине».

Первый пуш пакета на GHCR часто **приватный**. Пока пакет приватный, VPS не скачает образ без логина. После первого `image` пакет нужно сделать публичным (шаг в инструкции ниже).

---

## Почему так, а не иначе

| Вариант | Почему нет (для этой витрины) |
|---|---|
| Shared hosting REG.RU | Нет Java/Spring/Docker/Postgres 16 |
| systemd + `java -jar` на хосте | Рабочий план, но JRE и Postgres расходятся с ноутбуком; откат ручной; мы уже держим Postgres в Docker локально |
| `git pull && mvn` на VPS | Сборка и секреты на проде, долго, легко сломать живой сайт тестом |
| nginx + certbot | Больше ручной работы, чем Caddy, при той же схеме «прокси на 8080» |
| Kubernetes / Docker Swarm | Один процесс сайта, один VPS. Оркестратор не даёт пользы |
| Portainer | Лишняя панель с паролем в браузере; деплой всё равно из Actions по SSH |
| Buildpacks / Paketo вместо Dockerfile | Образ «сам соберётся», но HEALTHCHECK и прозрачность рецепта хуже; Caddy и Postgres всё равно Compose |
| GitHub Pages для Vue | У нас не статический сайт: API, сессия, OAuth, серверный рендер маршрутов |
| Открыть 8080 и 5432 в интернет | База и внутренний HTTP сайта не должны быть с улицы |
| Пароль БД `tayviscon` в проде | Это локальный default из `docker-compose.yml` |

Выбрано: **один VPS + Docker Compose + образ в GHCR + Caddy**. Это совпадает с тем, что REG.RU реально продаёт как «хостинг Docker»: не PaaS, а VPS с Docker Engine.

---

## Что уже в git (делает CI и репозиторий)

| Файл | Зачем |
|---|---|
| `Dockerfile` | Рецепт образа: Temurin 21, слои JAR, пользователь `spring`, проверка `GET /` |
| `.dockerignore` | В контекст сборки попадает только fat JAR, не весь git |
| `compose.prod.yml` | Три контейнера прода; без публикации 8080/5432 |
| `deploy/Caddyfile` | HTTPS и `reverse_proxy app:8080` |
| `.github/workflows/ci.yml` | verify / image / deploy |
| `docker-compose.yml` | Только локальный Postgres. На VPS не копировать |

Локально: `docker compose up -d`, затем `mvn -pl tayviscon-app -am spring-boot:run`.

После пуша в `main`: verify → JAR как artifact → образ в GHCR → SCP конфигов → `docker compose pull && up -d`.

## Ручные шаги

Репозиторий `tayviscon-io/tayviscon-site` уже создан, `origin` указывает на него. Пока нет VPS и SSH-секретов, пуш в `main` прогонит verify и сборку образа, а job `deploy` упадёт.

### 1. GitHub-репозиторий

Создавать репозиторий и пушить `main` заново не нужно. Осталось на GitHub: environment `production`, секреты SSH, публичность GHCR — шаги ниже.

### 2. VPS REG.RU

1. Ключ ноутбука: `ssh-keygen -t ed25519`. **Публичный** ключ — в панель Cloud VPS.
2. Заказ: 2 vCPU / **4 GB RAM**, Ubuntu **24.04**, приложение **Docker**. Скопируйте IPv4. 2 GB мало: Java + Postgres + Caddy.
3. Первый SSH. Потом отключите вход по паролю.
4. Файрвол только в ОС (у Cloud VPS нет security groups):

```bash
sudo ufw allow 22
sudo ufw allow 80
sudo ufw allow 443
sudo ufw enable
```

Не открывайте 8080 и 5432.

5. DNS: A `@` и `www` на этот IPv4 (`ns1.reg.ru` / `ns2.reg.ru`). Caddy получит сертификат, только когда имя уже резолвится в IP сервера.

### 3. Пользователь и каталог на VPS

```bash
sudo adduser --disabled-password --gecos "" deploy
sudo usermod -aG docker deploy
sudo mkdir -p /opt/tayviscon/deploy
sudo chown -R deploy:deploy /opt/tayviscon
```

Ключ только для Actions (на доверенной машине):

```bash
ssh-keygen -t ed25519 -C "github-actions-tayviscon-site" -f tayviscon-deploy -N ""
```

Публичный — в `/home/deploy/.ssh/authorized_keys`. Приватный — только в GitHub secret.

### 4. Секреты приложения на VPS

Файл `/opt/tayviscon/.env`, права `600`. Пароль БД **не** `tayviscon`.

```bash
SITE_HOST=example.com
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/tayviscon
SPRING_DATASOURCE_USERNAME=tayviscon
SPRING_DATASOURCE_PASSWORD=
GITHUB_OAUTH_CLIENT_ID=
GITHUB_OAUTH_CLIENT_SECRET=
GITHUB_TOKEN=
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
```

`GITHUB_TOKEN` здесь — fine-grained PAT с `Contents: Read` на **оба** репозитория: `tayviscon-io/yet-another-course` и `tayviscon-io/tayviscon-knowledge-base`. Это не токен Actions.

### 5. GitHub environment и SSH

Repo → Settings → Environments → `production`:

| Имя | Значение |
|---|---|
| `SSH_HOST` | IPv4 или DNS VPS |
| `SSH_USER` | `deploy` |
| `SSH_PRIVATE_KEY` | приватный `tayviscon-deploy` |

Опционально позже: `SSH_FINGERPRINT` (SHA256 host key) — сейчас workflow его не передаёт: клиент appleboy на GitHub runner не совпадал с отпечатком OpenSSH на Ubuntu 24.04.

Не создавайте user-secrets `GITHUB_TOKEN` / `GITHUB_OAUTH_*`. Пароль панели REG.RU сюда не класть.

Встроенный `secrets.GITHUB_TOKEN` у job `image` — служебный токен GitHub, чтобы запушить образ в GHCR. Это не ваш PAT.

### 6. OAuth и PAT каталога

1. GitHub → Developer settings → OAuth Apps. Homepage `https://<SITE_HOST>`. Callback **`https://<SITE_HOST>/login/oauth2/code/github`**. Для ноутбука добавьте `http://localhost:8080/login/oauth2/code/github` или заведите второе приложение.
2. Client ID/secret — в `/opt/tayviscon/.env`.
3. Fine-grained PAT: name например `tayviscon-site-catalog`, owner `tayviscon-io`, repositories **`yet-another-course` и `tayviscon-knowledge-base`**, permission **Contents: Read-only**. На VPS имя переменной **`GITHUB_TOKEN`**. Копия в GitHub, если нужна, только как **`CATALOG_GITHUB_TOKEN`**; workflow её не читает.

### 7. Видимость пакета GHCR

Пакет может оставаться **private**: job `deploy` логинится на `ghcr.io` служебным `GITHUB_TOKEN` с `packages: read`, тянет образ и делает `docker logout`. Делать пакет Public не обязательно.

### 8. Первый подъём

Когда образ уже в GHCR и `.env` заполнен:

```bash
cd /opt/tayviscon
docker compose -f compose.prod.yml --env-file .env up -d
```

Или дождитесь первого зелёного `deploy` с `main`.

Проверка: `https://<SITE_HOST>/` — витрина, `/api/courses` — JSON, «Войти» возвращается на prod callback. `ss -tlnp` и `ufw status` — нет публичных 8080/5432.

## Откат

```bash
cd /opt/tayviscon
export IMAGE_TAG=<старый-git-sha>
docker compose -f compose.prod.yml --env-file .env pull app
docker compose -f compose.prod.yml --env-file .env up -d
```

Тома с данными Postgres и сертификатами Caddy не удалять.

## Чего не делать

- Копировать локальный `docker-compose.yml` на VPS.
- Ставить Java и запускать `mvn` на сервере.
- Публиковать 8080/5432; использовать пароль `tayviscon` в проде.
- Класть OAuth, PAT или пароль БД в workflow или в Docker-образ.
- Kubernetes, Swarm, Portainer, Redis, Vault — для этой витрины не нужны.
