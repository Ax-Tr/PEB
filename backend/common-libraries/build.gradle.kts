plugins {
    id("peb.java-conventions")
    id("peb.testing")
    // io.spring.dependency-management is applied transitively by peb.testing.
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}")
    }
}

dependencies {
    // Web + MVC (filters, exception handling) — not the full starter to keep the lib light.
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    // Persistence (outbox / idempotency / audit entities + repositories)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Messaging
    implementation("org.springframework.kafka:spring-kafka")
    // Validation + JSON
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    // IDs + logging
    implementation(libs.ulid.creator)
    implementation("org.slf4j:slf4j-api")
    compileOnly("ch.qos.logback:logback-classic")

    testImplementation("ch.qos.logback:logback-classic")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
}
