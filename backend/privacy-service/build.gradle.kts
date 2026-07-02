plugins {
    id("peb.spring-service")
}

dependencies {
    implementation(project(":common-libraries"))

    // Persistence + migrations (service owns privacy_db)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Security (resource server)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Eventing: emits DSR_RECEIVED / DATA_ERASURE_REQUESTED / DSR_COMPLETED / DPDP_GRIEVANCE_RAISED
    // so each service can anonymise/retain its own slice of the data principal's data.
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
