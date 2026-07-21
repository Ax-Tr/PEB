plugins {
    id("peb.spring-service")
}

dependencies {
    implementation(project(":common-libraries"))
    implementation(project(":customer-service"))
    implementation(project(":vendor-service"))
    implementation(project(":employee-payroll-service"))
    implementation(project(":product-service"))
    implementation(project(":invoice-gst-service"))
    implementation(project(":purchase-expense-service"))
    implementation(project(":commitment-service"))
    implementation(project(":notification-service"))
    implementation(project(":ocr-document-service"))
    implementation(project(":ca-collaboration-service"))

    // Persistence + migrations (service owns business_db)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Cache / rate-limit
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Security: validates access tokens issued by identity-service (resource server)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Eventing
    implementation("org.springframework.kafka:spring-kafka")

    // Object storage for OCR/documents
    implementation("software.amazon.awssdk:s3:2.28.0")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
