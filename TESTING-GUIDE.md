# Руководство по запуску и проверке системы

## Шаг 1: Подготовка

### 1.1 Создать Docker сеть (если еще не создана)
```bash
docker network create appnet
```

### 1.2 Проверить, что порты свободны
Убедитесь, что следующие порты не заняты:
- 8080 - API Gateway
- 8081 - Keycloak
- 8082 - Demo Service
- 8083 - Auth Service
- 8084 - File Service
- 8085 - Processing Service
- 3000 - Grafana
- 5432 - PostgreSQL (app)
- 5433 - PostgreSQL (keycloak)
- 5672 - RabbitMQ
- 6379 - Redis
- 9000 - MinIO API
- 9001 - MinIO Console
- 9090 - Prometheus
- 15672 - RabbitMQ Management

## Шаг 2: Запуск Keycloak

```bash
cd KeyCloak
docker compose -f docker-compose.keycloak.yml up -d
cd ..
```

Проверка:
```bash
# Проверить, что Keycloak запустился
curl http://localhost:8081/realms/rs-exam-project/protocol/openid-connect/certs
```

**Настройка Keycloak:**
1. Откройте http://localhost:8081 (логин: admin, пароль: admin)
2. Создайте realm `rs-exam-project` (если еще не создан)
3. Создайте client `exam-client`:
   - Client ID: `exam-client`
   - Client Protocol: `openid-connect`
   - Access Type: `confidential`
   - Valid Redirect URIs: `*`
   - Direct Access Grants Enabled: `ON`
   - Сохраните и скопируйте Client Secret в docker-compose.yml

## Шаг 3: Запуск всех сервисов

**ВАЖНО:** Если вы внесли изменения в код или зависимости, необходимо пересобрать образы:

```bash
# Вариант 1: Собрать и запустить одной командой (рекомендуется)
docker compose up -d --build

# Вариант 2: Сначала собрать, потом запустить
docker compose build
docker compose up -d

# Вариант 3: Пересобрать только измененные сервисы
docker compose build file-service processing-service
docker compose up -d
```

**Если образы уже собраны и код не менялся**, можно использовать:
```bash
docker compose up -d
```

Проверить статус всех контейнеров:
```bash
docker compose ps
```

Проверить логи сервисов:
```bash
# API Gateway
docker compose logs -f gateway

# File Service
docker compose logs -f file-service

# Processing Service
docker compose logs -f processing-service

# Все сервисы
docker compose logs -f
```

## Шаг 4: Проверка инфраструктуры

### 4.1 RabbitMQ Management
- URL: http://localhost:15672
- Логин: `rabbit`
- Пароль: `rabbitpass`
- Проверьте, что очередь `file.processing` создана (должна появиться после первого использования)

### 4.2 MinIO Console
- URL: http://localhost:9001
- Логин: `minio`
- Пароль: `minio12345`
- Проверьте, что bucket `files` существует

### 4.3 Prometheus
- URL: http://localhost:9090
- Проверьте targets: Status → Targets
- Все сервисы должны быть UP

### 4.4 Grafana
- URL: http://localhost:3000
- Логин: `admin`
- Пароль: `admin`
- Добавьте Prometheus как источник данных: http://prometheus:9090

## Шаг 5: Проверка API

### 5.1 Регистрация и получение токена

```bash
# Регистрация нового пользователя
curl -X POST http://localhost:8083/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'

# Вход и получение токена
curl -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

Сохраните `access_token` из ответа в переменную:
```bash
# Linux/Mac
export ACCESS_TOKEN="your_token_here"

# Windows PowerShell
$env:ACCESS_TOKEN="your_token_here"

# Windows CMD
set ACCESS_TOKEN=your_token_here
```

### 5.2 Проверка File Service через API Gateway

```bash
# Загрузка файла
curl -X POST http://localhost:8080/api/files/files/upload \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "file=@test.txt"

# Проверка существования файла
curl -X GET http://localhost:8080/api/files/files/exists/{fileName} \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# Скачивание файла
curl -X GET http://localhost:8080/api/files/files/download/{fileName} \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -o downloaded_file.txt

# Удаление файла
curl -X DELETE http://localhost:8080/api/files/files/{fileName} \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 5.3 Проверка работы с RabbitMQ

После загрузки или удаления файла проверьте:

1. **RabbitMQ Management UI** (http://localhost:15672):
   - Перейдите в Queues
   - Найдите очередь `file.processing`
   - Проверьте, что сообщения обрабатываются (Messages готовы/обработаны)

2. **Логи Processing Service**:
```bash
docker compose logs -f processing-service
```

Вы должны увидеть сообщения типа:
```
Received processing request: fileName=..., operation=upload
Processing uploaded file: ...
File ... processed successfully
```

### 5.4 Проверка метрик Prometheus

Проверьте метрики каждого сервиса:

```bash
# API Gateway метрики
curl http://localhost:8080/actuator/prometheus

# File Service метрики
curl http://localhost:8084/actuator/prometheus

# Processing Service метрики
curl http://localhost:8085/actuator/prometheus

# Auth Service метрики
curl http://localhost:8083/actuator/prometheus

# Demo Service метрики
curl http://localhost:8082/actuator/prometheus
```

## Шаг 6: Полная проверка workflow

### 6.1 Загрузка и обработка файла

1. **Загрузите файл:**
```bash
echo "Test file content" > test.txt
curl -X POST http://localhost:8080/api/files/files/upload \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "file=@test.txt"
```

2. **Проверьте MinIO:**
   - Откройте http://localhost:9001
   - Зайдите в bucket `files`
   - Убедитесь, что файл загружен

3. **Проверьте RabbitMQ:**
   - Откройте http://localhost:15672
   - Проверьте очередь `file.processing`
   - Сообщение должно быть обработано

4. **Проверьте логи Processing Service:**
```bash
docker compose logs processing-service | grep -i "processing"
```

### 6.2 Удаление файла

```bash
# Удалите файл (замените {fileName} на реальное имя файла)
curl -X DELETE http://localhost:8080/api/files/files/{fileName} \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Проверьте логи Processing Service - должно появиться сообщение о обработке удаления.

## Шаг 7: Проверка мониторинга

### 7.1 Настройка Grafana Dashboard

1. Войдите в Grafana (http://localhost:3000)
2. Добавьте Prometheus как Data Source:
   - URL: http://prometheus:9090
   - Access: Server (default)
   - Сохраните и протестируйте

3. Создайте dashboard или импортируйте готовый:
   - Перейдите в Dashboards → New → Import
   - Можно использовать готовые dashboard для Spring Boot

### 7.2 Проверка метрик в Prometheus

1. Откройте http://localhost:9090
2. Перейдите в Graph
3. Попробуйте запросы:
   - `jvm_memory_used_bytes{application="file-service"}`
   - `http_server_requests_seconds_count{application="file-service"}`
   - `rabbitmq_queue_messages_ready`

## Возможные проблемы и решения

### Проблема: Сервисы не могут подключиться к Keycloak
**Решение:** Убедитесь, что Keycloak запущен и доступен на порту 8081

### Проблема: Ошибка подключения к RabbitMQ
**Решение:** Проверьте логи: `docker compose logs rabbitmq`

### Проблема: Ошибка подключения к MinIO
**Решение:** 
- Проверьте, что MinIO запущен: `docker compose ps minio`
- Проверьте логи: `docker compose logs minio`

### Проблема: Файлы не загружаются в MinIO
**Решение:**
- Убедитесь, что bucket `files` создан
- Проверьте credentials в docker-compose.yml
- Проверьте логи file-service

### Проблема: Processing Service не обрабатывает сообщения
**Решение:**
- Проверьте, что `@EnableRabbit` добавлен в ProcessingServiceApplication
- Проверьте логи: `docker compose logs processing-service`
- Убедитесь, что очередь создана в RabbitMQ

### Проблема: Метрики не собираются Prometheus
**Решение:**
- Проверьте prometheus.yml - все targets должны быть указаны
- Проверьте, что actuator endpoints доступны
- Проверьте логи Prometheus: `docker compose logs prometheus`

## Остановка системы

```bash
# Остановить все сервисы
docker compose down

# Остановить Keycloak
cd KeyCloak
docker compose -f docker-compose.keycloak.yml down
cd ..

# Удалить все контейнеры и volumes (ОСТОРОЖНО: удалит все данные)
docker compose down -v
cd KeyCloak
docker compose -f docker-compose.keycloak.yml down -v
cd ..
```

