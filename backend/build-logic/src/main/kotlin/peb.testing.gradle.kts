// Shared test stack: JUnit 5, AssertJ, Mockito, Testcontainers (real Postgres/Kafka/Redis).
plugins {
    java
    id("io.spring.dependency-management")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencyManagement {
    imports {
        mavenBom(libs.testcontainers.bom.get().toString())
    }
}

dependencies {
    "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    "testImplementation"("org.assertj:assertj-core")
    "testImplementation"("org.testcontainers:junit-jupiter")
    "testImplementation"("org.testcontainers:postgresql")
    "testImplementation"("org.testcontainers:kafka")
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
    systemProperty("file.encoding", "UTF-8")
}
