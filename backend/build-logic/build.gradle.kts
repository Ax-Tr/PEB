plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // Marker artifacts let precompiled script plugins apply these plugins by id.
    implementation(libs.plugin.springBoot)
    implementation(libs.plugin.springDepMgmt)
    implementation(libs.plugin.spotless)

    // Expose the generated version-catalog accessors (LibrariesForLibs) to the precompiled
    // script plugins so they can reference `libs.*` (standard Gradle workaround).
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
