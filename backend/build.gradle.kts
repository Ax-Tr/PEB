// Root build. Cross-cutting config only; each module applies its own convention plugin.
plugins {
    base
}

allprojects {
    group = "com.paywithease"
    version = "0.1.0-SNAPSHOT"
}

tasks.register("printModules") {
    group = "help"
    description = "Lists all Gradle modules in the PEB backend."
    doLast { subprojects.forEach { println(it.path) } }
}
