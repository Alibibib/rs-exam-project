# rs-exam-project

Monorepo с несколькими микросервисами (Spring Boot 3.2, Java 17) и отдельным Keycloak для аутентификации.

## Состав
- `KeyCloak/docker-compose.keycloak.yml` — Keycloak (порт 8081 на хосте, `appnet` сеть).
- `api-gateway/` — Spring Cloud Gateway + resource server (порт 8080).
- `DemoService/` — демо ресурсный сервис с публичным/приватным эндпоинтом (порт 8082).
- `AuthService/` — прокси к Keycloak для регистрации/логина (порт 8083).
- Доп. заготовки: `file-service/`, `processing-service/` (пустые болванки).

## Быстрый старт (Docker)
1) Однажды создать сеть:
   ```bash
   docker network create appnet
   ```
2) Запустить Keycloak:
   ```bash
   cd KeyCloak
   docker compose -f docker-compose.keycloak.yml up -d
   ```
   Проверка JWKS: `curl http://localhost:8081/realms/rs-exam-project/protocol/openid-connect/certs`
3) Запустить сервисы:
   ```bash
   cd ..
   docker compose up -d
   ```
4) Получить токен (через AuthService):
   - `POST http://localhost:8083/auth/register` body: `{"username","email","password"}`
   - `POST http://localhost:8083/auth/login` body: `{"username","password"}` → вернёт `access_token`
5) Проверить приватный эндпоинт через gateway:
   ```bash
   ACCESS_TOKEN=...
   curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/demo/private
   ```

## Настройка Keycloak
- Realm: `rs-exam-project`
- Client: `exam-client` (Confidential)
  - `client-secret`: задайте/подставьте в env `KEYCLOAK_CLIENT_SECRET`
  - `Direct access grants (Resource Owner Password)` — ON (нужно для `/auth/login`)
- Админ учётка: `admin/admin` (из compose)
- Импорт realm: файл `realm-export.json` отсутствует, но параметры выше достаточны для ручной настройки.

## Переменные окружения (важно для issuer)
Все сервисы используют плейсхолдеры:
- `KEYCLOAK_ISSUER_URI` — для gateway и DemoService (по умолчанию `http://keycloak:8080/realms/rs-exam-project`).
- `KEYCLOAK_BASE_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`, `KEYCLOAK_ADMIN_USERNAME`, `KEYCLOAK_ADMIN_PASSWORD` — для AuthService (дефолты совпадают с Docker).

Локальный запуск без Docker Keycloak (порт 8081 на хосте):
- `KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/rs-exam-project`
- `KEYCLOAK_BASE_URL=http://localhost:8081`

## API
### AuthService (`http://localhost:8083`)
- `POST /auth/register` — body: `{"username","email","password"}` → 200 OK
- `POST /auth/login` — body: `{"username","password"}` → `{"access_token","refresh_token","token_type","expires_in"}`

### Gateway-прокси (`http://localhost:8080`)
- `/api/auth/**` → AuthService (фильтр `StripPrefix=1`)
- `/api/demo/**` → DemoService (фильтр `StripPrefix=2`)
- `/login`, `/register` → проксируются в `/auth/<...>` на AuthService

### DemoService (за gateway)
- `GET /api/demo/public` — без авторизации
- `GET /api/demo/private` — требует Bearer JWT, возвращает строку с `sub/username/email`

## Как добавить новый микросервис
1) Подключить к сети `appnet` в Docker-compose.
2) В самом сервисе:
   - Добавить зависимости: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`.
   - Настроить `issuer-uri` через env `KEYCLOAK_ISSUER_URI` (как в DemoService).
3) В gateway (`application.yml`):
   - Добавить route с `uri: http://<service-name>:<port>`
   - Пропишите `Path` и `StripPrefix`/`RewritePath` по необходимости.
4) Перезапустить gateway + новый сервис.

## Частые проблемы
- 500 на приватных эндпоинтах: чаще всего mismatch issuer. Убедитесь, что `KEYCLOAK_ISSUER_URI` совпадает с `iss` токена и Keycloak доступен по хосту/порту из контейнера.
- DNS `keycloak` не резолвится: проверьте, что сеть `appnet` существует и оба compose подключены.
- Ошибка логина: проверьте `client-secret` в AuthService и в Keycloak для клиента `exam-client`.

