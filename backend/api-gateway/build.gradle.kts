// API Gateway is reactive (Spring Cloud Gateway / WebFlux) so it does NOT use peb.spring-service
// (which brings servlet web). It applies the base Java conventions plus Boot + Spring Cloud.
plugins {
    id("peb.java-conventions")
    id("peb.testing")
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDepMgmt)
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.bom.get().toString())
    }
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Edge authentication: validate access tokens (JWKS from identity-service)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    // Redis-backed distributed rate limiting at the edge
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation(libs.logstash.encoder)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("")
}
tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
    enabled = false
}
