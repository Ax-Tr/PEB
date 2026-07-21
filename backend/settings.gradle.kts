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

// Shared library module
include("common-libraries")

// Identity domain submodules
include("tenant-service")
include("privacy-service")

// Business domain submodules
include("customer-service")
include("vendor-service")
include("employee-payroll-service")
include("product-service")
include("invoice-gst-service")
include("purchase-expense-service")
include("commitment-service")
include("notification-service")
include("ocr-document-service")
include("ca-collaboration-service")

// Finance domain submodules
include("payment-collection-service")
include("payout-service")
include("accounting-ledger-service")
include("installment-service")
include("transaction-ingestion-service")
include("reconciliation-service")
include("compliance-report-service")
include("analytics-service")
include("audit-evidence-service")
include("ai-automation-service")

// Deployable services — consolidated 4-service architecture
include("api-gateway")
include("identity-service")
include("business-service")
include("finance-service")
