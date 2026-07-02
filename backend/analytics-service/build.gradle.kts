plugins {
    id("peb.spring-service")
}

dependencies {
    implementation(project(":common-libraries"))

    // Persistence + migrations (service owns analytics_db — an OLAP-shaped read-model store).
    // NOTE: production target is ClickHouse for the materialized aggregates; the read-model tables
    // here are denormalised, append-oriented and engine-agnostic so the store can be swapped without
    // touching the query/compute layer. PostgreSQL is used now because it is the only engine
    // verifiable in this environment.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Security (resource server)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Eventing: consumes invoice/payment/purchase/payout/installment/ledger events (read-only,
    // event-fed — analytics never queries the OLTP services' databases).
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
