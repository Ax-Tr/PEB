plugins {
    id("peb.spring-service")
}

dependencies {
    implementation(project(":common-libraries"))

    // Persistence + migrations (service owns ai_automation_db)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Security (resource server)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Eventing: consumes transaction/financial events to raise governed AI suggestions/alerts;
    // emits AI_SUGGESTION_CREATED / ANOMALY_DETECTED via the transactional outbox.
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
