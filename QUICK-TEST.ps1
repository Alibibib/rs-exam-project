# PowerShell скрипт для быстрого тестирования системы
# Использование: .\QUICK-TEST.ps1

Write-Host "=== Тестирование микросервисной системы ===" -ForegroundColor Green

# Цвета для вывода
$successColor = "Green"
$errorColor = "Red"
$infoColor = "Cyan"

# Функция для проверки HTTP endpoint
function Test-Endpoint {
    param(
        [string]$Url,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [string]$Body = $null
    )
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $Headers
            TimeoutSec = 10
            ErrorAction = "Stop"
        }
        
        if ($Body) {
            $params.Body = $Body
            $params.ContentType = "application/json"
        }
        
        $response = Invoke-WebRequest @params
        return @{
            Success = $true
            StatusCode = $response.StatusCode
            Content = $response.Content
        }
    }
    catch {
        return @{
            Success = $false
            StatusCode = $_.Exception.Response.StatusCode.value__
            Error = $_.Exception.Message
        }
    }
}

# 1. Проверка инфраструктуры
Write-Host "`n[1/7] Проверка инфраструктуры..." -ForegroundColor $infoColor

$infra = @{
    "Keycloak" = "http://localhost:8081/realms/rs-exam-project/protocol/openid-connect/certs"
    "RabbitMQ Management" = "http://localhost:15672"
    "MinIO Console" = "http://localhost:9001"
    "Prometheus" = "http://localhost:9090"
}

foreach ($service in $infra.Keys) {
    $result = Test-Endpoint -Url $infra[$service]
    if ($result.Success) {
        Write-Host "  ✓ $service доступен" -ForegroundColor $successColor
    }
    else {
        Write-Host "  ✗ $service недоступен (код: $($result.StatusCode))" -ForegroundColor $errorColor
    }
}

# 2. Проверка сервисов
Write-Host "`n[2/7] Проверка микросервисов..." -ForegroundColor $infoColor

$services = @{
    "API Gateway" = "http://localhost:8080/actuator/health"
    "Auth Service" = "http://localhost:8083/actuator/health"
    "File Service" = "http://localhost:8084/actuator/health"
    "Processing Service" = "http://localhost:8085/actuator/health"
    "Demo Service" = "http://localhost:8082/actuator/health"
}

foreach ($service in $services.Keys) {
    $result = Test-Endpoint -Url $services[$service]
    if ($result.Success) {
        Write-Host "  ✓ $service доступен" -ForegroundColor $successColor
    }
    else {
        Write-Host "  ✗ $service недоступен" -ForegroundColor $errorColor
    }
}

# 3. Регистрация пользователя
Write-Host "`n[3/7] Регистрация тестового пользователя..." -ForegroundColor $infoColor

$registerBody = @{
    username = "testuser_$(Get-Random)"
    email = "test_$(Get-Random)@example.com"
    password = "TestPassword123!"
} | ConvertTo-Json

$registerResult = Test-Endpoint -Url "http://localhost:8083/auth/register" -Method "POST" -Body $registerBody

if ($registerResult.Success) {
    Write-Host "  ✓ Пользователь зарегистрирован" -ForegroundColor $successColor
    $userData = $registerBody | ConvertFrom-Json
}
else {
    if ($registerResult.StatusCode -eq 409) {
        Write-Host "  ⚠ Пользователь уже существует, используем существующие данные" -ForegroundColor "Yellow"
        $userData = @{
            username = "testuser"
            password = "password123"
        }
    }
    else {
        Write-Host "  ✗ Ошибка регистрации: $($registerResult.Error)" -ForegroundColor $errorColor
        exit 1
    }
}

# 4. Получение токена
Write-Host "`n[4/7] Получение access token..." -ForegroundColor $infoColor

$loginBody = @{
    username = $userData.username
    password = if ($userData.password) { $userData.password } else { "TestPassword123!" }
} | ConvertTo-Json

$loginResult = Test-Endpoint -Url "http://localhost:8083/auth/login" -Method "POST" -Body $loginBody

if ($loginResult.Success) {
    $tokenData = $loginResult.Content | ConvertFrom-Json
    $accessToken = $tokenData.access_token
    Write-Host "  ✓ Токен получен" -ForegroundColor $successColor
}
else {
    Write-Host "  ✗ Ошибка входа: $($loginResult.Error)" -ForegroundColor $errorColor
    exit 1
}

# 5. Тестирование File Service
Write-Host "`n[5/7] Тестирование File Service..." -ForegroundColor $infoColor

$headers = @{
    "Authorization" = "Bearer $accessToken"
}

# Проверка существования файла (ожидаем 404)
$existsResult = Test-Endpoint -Url "http://localhost:8080/api/files/files/exists/nonexistent.txt" -Headers $headers
if ($existsResult.Success -or $existsResult.StatusCode -eq 404) {
    Write-Host "  ✓ Endpoint проверки существования файла работает" -ForegroundColor $successColor
}
else {
    Write-Host "  ✗ Endpoint проверки существования файла не работает" -ForegroundColor $errorColor
}

# 6. Проверка Prometheus метрик
Write-Host "`n[6/7] Проверка метрик Prometheus..." -ForegroundColor $infoColor

$prometheusEndpoints = @(
    "http://localhost:8080/actuator/prometheus",
    "http://localhost:8084/actuator/prometheus",
    "http://localhost:8085/actuator/prometheus"
)

foreach ($endpoint in $prometheusEndpoints) {
    $result = Test-Endpoint -Url $endpoint
    if ($result.Success -and $result.Content -like "*jvm_*") {
        Write-Host "  ✓ Метрики доступны: $endpoint" -ForegroundColor $successColor
    }
    else {
        Write-Host "  ✗ Метрики недоступны: $endpoint" -ForegroundColor $errorColor
    }
}

# 7. Проверка RabbitMQ
Write-Host "`n[7/7] Проверка RabbitMQ..." -ForegroundColor $infoColor

$rabbitmqResult = Test-Endpoint -Url "http://localhost:15672/api/overview" -Headers @{
    "Authorization" = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("rabbit:rabbitpass"))
}

if ($rabbitmqResult.Success) {
    Write-Host "  ✓ RabbitMQ Management API доступен" -ForegroundColor $successColor
}
else {
    Write-Host "  ✗ RabbitMQ Management API недоступен" -ForegroundColor $errorColor
}

Write-Host "`n=== Тестирование завершено ===" -ForegroundColor Green
Write-Host "`nДля детального тестирования см. TESTING-GUIDE.md" -ForegroundColor $infoColor

