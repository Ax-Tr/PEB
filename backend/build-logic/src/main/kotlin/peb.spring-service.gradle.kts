// Convention for a deployable Spring Boot microservice.
plugins {
    id("peb.java-conventions")
    id("peb.testing")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.bom.get().toString())
    }
}

dependencies {
    add("implementation", "org.springframework.boot:spring-boot-starter-web")
    add("implementation", "org.springframework.boot:spring-boot-starter-validation")
    add("implementation", "org.springframework.boot:spring-boot-starter-actuator")
    add("implementation", libs.springdoc)
    add("implementation", libs.resilience4j.spring.boot3)
    add("implementation", libs.logstash.encoder)
    add("implementation", libs.mapstruct)
    add("annotationProcessor", libs.mapstruct.processor)

    // Observability (traces/metrics/logs via OTel + Micrometer/Prometheus)
    add("implementation", "io.micrometer:micrometer-registry-prometheus")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("")
}
tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
    enabled = false
}
