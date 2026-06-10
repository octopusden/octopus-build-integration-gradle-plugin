plugins {
    kotlin("jvm") version "1.9.25"
    id("org.octopusden.octopus-build-integration")
}

releaseManagement {
    releaseDependencies {
        component("component_a", "1.0.0")
        component("component_b:1.1.0")
    }

    outputFile.set(project.layout.buildDirectory.file("123.json"))

    scan {
        enabled.set(false)
        componentsRegistryUrl.set("123")
        projects.set("123")
        configurations.set("123")
    }
}

dependencies {
    implementation("org.octopusden.octopus-cloud-commons:octopus-security-common:2.0.15")
}
