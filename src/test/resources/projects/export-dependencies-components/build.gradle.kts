plugins {
    kotlin("jvm") version "1.9.25"
    id("org.octopusden.octopus-build-integration")
}

buildIntegration {
    exportDependencies {
        components {
            include("a", "1.0.0")
            include("b", "1.1.0")
        }
    }
}

dependencies {
    implementation("org.octopusden.octopus.infrastructure:components-registry-service-client:2.0.62")
    implementation("org.octopusden.octopus-cloud-commons:octopus-security-common:2.0.15")
}