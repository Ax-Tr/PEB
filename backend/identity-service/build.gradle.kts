plugins {
    id("peb.spring-service")
}

dependencies {
    implementation(project(":common-libraries"))

    // Persistence + migrations (service owns identity_db)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Cache / OTP store / rate-limit / idempotency locks
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Security: RBAC + JWT (self-signed via Nimbus, validated as a resource server)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Eventing
    implementation("org.springframework.kafka:spring-kafka")

    // Integration tests use a real Postgres via Testcontainers
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
