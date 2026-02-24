# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Кешируем зависимости отдельным слоем
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Копируем исходники (maven.config исключён через .dockerignore — там плохой абс. путь)
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Runtime (jammy поддерживает arm64/Apple Silicon)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
