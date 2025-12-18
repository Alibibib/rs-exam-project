# Как проверить, что всё работает

## 1. Подготовка и запуск
- Убедитесь, что есть общий external network: `docker network create appnet` (достаточно один раз).
- Поднимите Keycloak (порт 8081 снаружи): `docker compose -f KeyCloak/docker-compose.keycloak.yml up -d`.
- Запустите остальной стек из корня репозитория: `docker compose up -d --build`. Первый запуск может быть долгим из‑за скачивания образов.
- Посмотреть логи конкретного сервиса: `docker compose logs -f processing-service` (или другое имя сервиса).

## 2. Быстрые проверки в браузере (Google/Chrome)
- Gateway health: http://localhost:8080/actuator/health
- Публичный эндпоинт демо: http://localhost:8080/api/demo/public (должен вернуть `public ok`).
- Консоль Keycloak: http://localhost:8081/ (логин/пароль admin/admin).
- Консоль MinIO: http://localhost:9001/ (логин/пароль minio/minio12345).
- RabbitMQ UI: http://localhost:15672/ (логин/пароль rabbit/rabbitpass).
- Grafana: http://localhost:3000/ (логин/пароль admin/admin).

## 3. Проверка через Postman
Используйте базовый URL Gateway: `http://localhost:8080`. Все запросы кроме логина/регистрации требуют Bearer JWT.

1) Регистрация пользователя  
POST `/api/auth/register`  
Body (JSON):  
```json
{
  "username": "tester",
  "password": "P@ssw0rd!",
  "email": "tester@example.com",
  "firstName": "Test",
  "lastName": "User"
}
```

2) Логин  
POST `/api/auth/login`  
Body (JSON):  
```json
{
  "username": "tester",
  "password": "P@ssw0rd!"
}
```  
Сохраните `accessToken` из ответа и добавьте в Postman: Authorization → Bearer Token.

3) Файлы (все пути через gateway)  
- Загрузка: POST `/api/files` (form-data, ключ `file`, тип File). Ответ содержит `id` и `objectKey`.  
- Список: GET `/api/files`  
- Метаданные: GET `/api/files/{id}`  
- Скачать: GET `/api/files/{id}/download`  
- Удалить: DELETE `/api/files/{id}`

4) Книги (простая CRUD обёртка)  
- Список: GET `/api/books`  
- Создать: POST `/api/books` с JSON `{"title":"Book 1","author":"Me","isbn":"123"}`  
- Обновить: PUT `/api/books/{id}` с тем же форматом тела  
- Удалить: DELETE `/api/books/{id}`

5) Закрытый демо-эндпоинт (для проверки JWT)  
GET `/api/demo/private` с тем же Bearer-токеном. В ответе будут `sub`, `username`, `email` из токена.

## 4. Проверка фоновой обработки файлов
- После загрузки файла (шаг 3.3) откройте логи: `docker compose logs -f processing-service`. Должны появиться строки вида `Received file event...` и `Processed file ... (N bytes)`.
- В MinIO UI (http://localhost:9001) проверьте, что объект появился в бакете `files`.
- В RabbitMQ UI можно убедиться, что очередь `file.process` существует и сообщения потребляются.

## 5. Остановка и очистка
- Остановить стек: `docker compose down` (из корня). Keycloak остановить отдельно: `docker compose -f KeyCloak/docker-compose.keycloak.yml down`.
- Если нужно очистить данные (Postgres/Redis/MinIO/RabbitMQ/Grafana/Keycloak), добавьте флаг `-v`, но это удалит всё сохранённое: `docker compose down -v`.
