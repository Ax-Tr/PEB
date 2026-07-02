plugins {
    id("peb.spring-service")
}

dependencies {
    implementation(project(":common-libraries"))

    // Persistence + migrations (service owns audit_evidence_db — an append-only, immutable store).
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Security (resource server)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Eventing: consumes cross-service events to build the immutable evidence/audit trail;
    // emits AUDIT_EVENT_RECORDED.
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
