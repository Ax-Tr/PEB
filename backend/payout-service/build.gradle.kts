plugins {
    id("peb.spring-service")
}

dependencies {
    implementation(project(":common-libraries"))

    // Persistence + migrations (service owns payout_db)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Security (resource server + step-up gated actions)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Eventing: emits PAYOUT_APPROVAL_* / VENDOR_PAYMENT_* events
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
