FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/*SNAPSHOT.jar app.jar
EXPOSE 8103
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
