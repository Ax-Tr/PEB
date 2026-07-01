// Base Java conventions shared by every PEB module: toolchain, formatting, quality, coverage.
plugins {
    java
    jacoco
    id("com.diffplug.spotless")
}

// Repositories are provided centrally by settings.gradle.kts (PREFER_SETTINGS).

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Treat warnings as errors, but exclude categories that are noisy on Spring/JPA code and
    // carry little value here (serialVersionUID bookkeeping, annotation processing, ctor escape).
    options.compilerArgs.addAll(
        listOf("-parameters", "-Xlint:all,-serial,-processing,-this-escape", "-Werror"))
}

spotless {
    java {
        googleJavaFormat("1.24.0")
        target("src/**/*.java")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Coverage gate. Target per engineering-standards.md is 80% business logic / 90% accounting.
// It ratchets up each sprint; during Sprint 0 scaffolding it is bootstrapped low and can be
// overridden per-module via the `pebCoverageMinimum` project property.
val coverageMinimum = (findProperty("pebCoverageMinimum") as String?)?.toBigDecimal()
    ?: "0.30".toBigDecimal()

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                minimum = coverageMinimum
            }
            // Exclude framework boilerplate that carries no business logic from the metric.
            excludes = listOf(
                "*.*Application",
                "*.config.*",
                "*.*Entity",
                "*.*Dto",
            )
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}
