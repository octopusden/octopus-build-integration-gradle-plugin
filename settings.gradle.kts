pluginManagement {
    plugins {
        kotlin("jvm") version settings.extra["kotlin.version"] as String
        id("io.github.gradle-nexus.publish-plugin") version extra["nexus-plugin.version"] as String
        id("org.octopusden.octopus.oc-template") version extra["octopus-oc-template.version"] as String
        id("com.jfrog.artifactory") version extra["com-jfrog-artifactory.version"] as String
    }
}
rootProject.name = "octopus-build-integration-gradle-plugin"