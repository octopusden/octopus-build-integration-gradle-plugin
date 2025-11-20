pluginManagement {
    plugins {
        kotlin("jvm") version settings.extra["kotlin.version"] as String
        id("io.github.gradle-nexus.publish-plugin") version settings.extra["nexus-plugin.version"] as String
    }
}
rootProject.name = "octopus-build-integration-gradle-plugin"