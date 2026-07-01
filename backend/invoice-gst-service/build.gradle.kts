plugins {
    id("peb.spring-service")
}

dependencies {
    implementation(project(":common-libraries"))

    // Persistence + migrations (service owns invoice_db)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Security (resource server)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Eventing (INVOICE_GENERATED, INVOICE_SENT)
    implementation("org.springframework.kafka:spring-kafka")

    // PDF generation for invoices/notes (LGPL/MPL, safe for commercial use)
    implementation("com.github.librepdf:openpdf:1.3.35")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
