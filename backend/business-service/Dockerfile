FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S peb && adduser -S peb -G peb
WORKDIR /app
COPY business-service/build/libs/*.jar app.jar
USER peb
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
