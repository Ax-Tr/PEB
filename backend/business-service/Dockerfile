# Stage 1: Build stage
FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN chmod +x gradlew && ./gradlew :business-service:bootJar --no-daemon

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S peb && adduser -S peb -G peb
WORKDIR /app
COPY --from=build /app/business-service/build/libs/*.jar app.jar
USER peb
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
