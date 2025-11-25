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
        gradleDependencies {
            includeAllDependencies = true
            excludeModule(group = "org.octopusden.octopus-cloud-commons")
            excludeModule(module = "components-registry-service-client")
            excludeModule(group = "org.octopusden.octopus.releng", module = "versions-api")
        }
    }
}

dependencies {
    implementation("org.octopusden.octopus.infrastructure:components-registry-service-client:2.0.62")
    implementation("org.octopusden.octopus-cloud-commons:octopus-security-common:2.0.15")
    implementation("org.octopusden.octopus.releng:versions-api:2.0.10")
}