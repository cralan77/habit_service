# Stage 1: Build the application with Gradle
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy project files
COPY . .

# Grant execute permission to Gradle wrapper and build executable bootJar
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy compiled jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Support dynamic PORT environment variable (defaults to 8083)
ENV PORT=8083
EXPOSE ${PORT}

# Optimize JVM memory limits for containerized environments
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
