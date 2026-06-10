plugins {
    kotlin("jvm") version "1.9.25"
    id("org.octopusden.octopus-build-integration")
}

releaseManagement {
    releaseDependencies {
        component("component_a", "1.0.0")
        component("component_b:1.1.0")
    }
}

dependencies {
    implementation("org.octopusden.octopus.infrastructure:components-registry-service-client:2.0.62")
    implementation("org.octopusden.octopus-cloud-commons:octopus-security-common:2.0.15")
    implementation("org.octopusden.octopus.releng:versions-api:2.0.10")
}
