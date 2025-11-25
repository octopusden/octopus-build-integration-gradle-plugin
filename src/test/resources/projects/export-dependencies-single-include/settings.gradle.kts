pluginManagement {
    plugins {
        id("org.octopusden.octopus-build-integration") version settings.extra["octopus-build-integration.version"] as String
    }
}

rootProject.name = "test-project"