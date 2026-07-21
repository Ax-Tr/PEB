FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S peb && adduser -S peb -G peb
WORKDIR /app
COPY api-gateway/build/libs/*.jar app.jar
USER peb
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
