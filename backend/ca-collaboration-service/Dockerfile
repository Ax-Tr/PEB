# Multi-stage build → distroless runtime (non-root).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :ca-collaboration-service:bootJar --no-daemon

FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app
COPY --from=build /workspace/ca-collaboration-service/build/libs/ca-collaboration-service.jar app.jar
EXPOSE 8099
USER nonroot
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
