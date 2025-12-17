import org.octopusden.octopus.build.integration.gradle.plugin.extension.DependenciesExtension.Component

plugins {
    kotlin("jvm") version "1.9.25"
    id("org.octopusden.octopus-build-integration")
}

buildIntegration {
    dependencies {
        components.add(Component("component_a", "1.0.0"))
        components.add(Component("component_b", "1.1.0"))

        scan {
            projects.set(".*:(service-a|service-b)")
        }
    }
}

dependencies {
    implementation("org.octopusden.octopus-cloud-commons:octopus-security-common:2.0.15")
}
