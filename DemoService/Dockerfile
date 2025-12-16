# ===== BUILD STAGE =====
FROM gradle:8.6-jdk17 AS build
WORKDIR /app

# Копируем все нужные файлы
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src

# Собираем проект
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]