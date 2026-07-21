plugins {
    id("peb.spring-service")
}

dependencies {
    implementation(project(":common-libraries"))
    implementation(project(":payment-collection-service"))
    implementation(project(":payout-service"))
    implementation(project(":accounting-ledger-service"))
    implementation(project(":installment-service"))
    implementation(project(":transaction-ingestion-service"))
    implementation(project(":reconciliation-service"))
    implementation(project(":compliance-report-service"))
    implementation(project(":analytics-service"))
    implementation(project(":audit-evidence-service"))
    implementation(project(":ai-automation-service"))

    // Persistence + migrations (service owns finance_db)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Cache / rate-limit
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Security: validates access tokens issued by identity-service
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Eventing
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
