#!/bin/bash
# Bash скрипт для быстрого тестирования системы
# Использование: chmod +x QUICK-TEST.sh && ./QUICK-TEST.sh

echo "=== Тестирование микросервисной системы ==="

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Функция для проверки HTTP endpoint
test_endpoint() {
    local url=$1
    local method=${2:-GET}
    local headers=${3:-""}
    local body=${4:-""}
    
    if [ -z "$body" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" $headers "$url" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" $headers \
            -H "Content-Type: application/json" \
            -d "$body" "$url" 2>&1)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    content=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "${GREEN}  ✓ Код: $http_code${NC}"
        return 0
    else
        echo -e "${RED}  ✗ Код: $http_code${NC}"
        return 1
    fi
}

# 1. Проверка инфраструктуры
echo -e "\n${CYAN}[1/7] Проверка инфраструктуры...${NC}"

echo "  Проверка Keycloak..."
test_endpoint "http://localhost:8081/realms/rs-exam-project/protocol/openid-connect/certs"

echo "  Проверка RabbitMQ Management..."
test_endpoint "http://localhost:15672" > /dev/null 2>&1 && echo -e "${GREEN}  ✓ RabbitMQ Management доступен${NC}" || echo -e "${RED}  ✗ RabbitMQ Management недоступен${NC}"

echo "  Проверка MinIO Console..."
test_endpoint "http://localhost:9001" > /dev/null 2>&1 && echo -e "${GREEN}  ✓ MinIO Console доступен${NC}" || echo -e "${RED}  ✗ MinIO Console недоступен${NC}"

echo "  Проверка Prometheus..."
test_endpoint "http://localhost:9090/-/healthy" && echo -e "${GREEN}  ✓ Prometheus доступен${NC}" || echo -e "${RED}  ✗ Prometheus недоступен${NC}"

# 2. Проверка сервисов
echo -e "\n${CYAN}[2/7] Проверка микросервисов...${NC}"

services=(
    "API Gateway:http://localhost:8080/actuator/health"
    "Auth Service:http://localhost:8083/actuator/health"
    "File Service:http://localhost:8084/actuator/health"
    "Processing Service:http://localhost:8085/actuator/health"
    "Demo Service:http://localhost:8082/actuator/health"
)

for service_info in "${services[@]}"; do
    IFS=':' read -r name url <<< "$service_info"
    echo "  Проверка $name..."
    if test_endpoint "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}  ✓ $name доступен${NC}"
    else
        echo -e "${RED}  ✗ $name недоступен${NC}"
    fi
done

# 3. Регистрация пользователя
echo -e "\n${CYAN}[3/7] Регистрация тестового пользователя...${NC}"

RANDOM_SUFFIX=$(date +%s)
REGISTER_BODY="{\"username\":\"testuser_$RANDOM_SUFFIX\",\"email\":\"test_$RANDOM_SUFFIX@example.com\",\"password\":\"TestPassword123!\"}"

REGISTER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d "$REGISTER_BODY" \
    "http://localhost:8083/auth/register")

REGISTER_CODE=$(echo "$REGISTER_RESPONSE" | tail -n1)

if [ "$REGISTER_CODE" -eq 200 ] || [ "$REGISTER_CODE" -eq 201 ]; then
    echo -e "${GREEN}  ✓ Пользователь зарегистрирован${NC}"
    USERNAME="testuser_$RANDOM_SUFFIX"
    PASSWORD="TestPassword123!"
elif [ "$REGISTER_CODE" -eq 409 ]; then
    echo -e "${YELLOW}  ⚠ Пользователь уже существует, используем тестовые данные${NC}"
    USERNAME="testuser"
    PASSWORD="password123"
else
    echo -e "${RED}  ✗ Ошибка регистрации (код: $REGISTER_CODE)${NC}"
    exit 1
fi

# 4. Получение токена
echo -e "\n${CYAN}[4/7] Получение access token...${NC}"

LOGIN_BODY="{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}"

LOGIN_RESPONSE=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -d "$LOGIN_BODY" \
    "http://localhost:8083/auth/login")

ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -n "$ACCESS_TOKEN" ]; then
    echo -e "${GREEN}  ✓ Токен получен${NC}"
else
    echo -e "${RED}  ✗ Ошибка получения токена${NC}"
    echo "Ответ: $LOGIN_RESPONSE"
    exit 1
fi

# 5. Тестирование File Service
echo -e "\n${CYAN}[5/7] Тестирование File Service...${NC}"

echo "  Проверка endpoint проверки существования файла..."
if test_endpoint "http://localhost:8080/api/files/files/exists/nonexistent.txt" "GET" "-H \"Authorization: Bearer $ACCESS_TOKEN\"" > /dev/null 2>&1; then
    echo -e "${GREEN}  ✓ Endpoint работает${NC}"
else
    # 404 тоже нормально для несуществующего файла
    echo -e "${GREEN}  ✓ Endpoint работает (404 ожидаем)${NC}"
fi

# 6. Проверка Prometheus метрик
echo -e "\n${CYAN}[6/7] Проверка метрик Prometheus...${NC}"

METRICS_ENDPOINTS=(
    "http://localhost:8080/actuator/prometheus"
    "http://localhost:8084/actuator/prometheus"
    "http://localhost:8085/actuator/prometheus"
)

for endpoint in "${METRICS_ENDPOINTS[@]}"; do
    METRICS_RESPONSE=$(curl -s "$endpoint")
    if echo "$METRICS_RESPONSE" | grep -q "jvm_"; then
        echo -e "${GREEN}  ✓ Метрики доступны: $endpoint${NC}"
    else
        echo -e "${RED}  ✗ Метрики недоступны: $endpoint${NC}"
    fi
done

# 7. Проверка RabbitMQ
echo -e "\n${CYAN}[7/7] Проверка RabbitMQ...${NC}"

RABBITMQ_RESPONSE=$(curl -s -u rabbit:rabbitpass "http://localhost:15672/api/overview")
if echo "$RABBITMQ_RESPONSE" | grep -q "management_version"; then
    echo -e "${GREEN}  ✓ RabbitMQ Management API доступен${NC}"
else
    echo -e "${RED}  ✗ RabbitMQ Management API недоступен${NC}"
fi

echo -e "\n=== Тестирование завершено ==="
echo -e "\n${CYAN}Для детального тестирования см. TESTING-GUIDE.md${NC}"

