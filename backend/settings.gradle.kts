pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Lets Gradle auto-download the JDK 21 toolchain if the build machine only has another JDK.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "peb-backend"

// Shared library module (not a deployable service)
include("common-libraries")

// Deployable microservices (Sprint 0 ships gateway + identity; others added per sprint)
include("api-gateway")
include("identity-service")
include("tenant-service")
include("customer-service")
include("vendor-service")
include("employee-payroll-service")
include("product-service")
include("payment-collection-service")
include("invoice-gst-service")
include("accounting-ledger-service")
// include("purchase-expense-service")  // Sprint 6
// include("payout-service")            // Sprint 6
// include("installment-service")       // Sprint 8
// include("notification-service")      // Sprint 9
// include("transaction-ingestion-service") // Sprint 10
// include("reconciliation-service")    // Sprint 11
// include("compliance-report-service") // Sprint 12
// include("analytics-service")         // Sprint 13
// include("audit-evidence-service")    // Sprint 14
// include("ca-collaboration-service")  // Sprint 14
// include("ai-automation-service")     // Sprint 15
// include("ocr-document-service")      // Sprint 6 / 15
// include("rules-engine-service")      // cross-cutting
