plugins {
    kotlin("jvm") version "1.9.25"
    id("org.octopusden.octopus-build-integration")
}

releaseManagement {
    releaseDependencies {
        component("component_a", "1.0.0")
        component("component_b:1.1.0")
    }

    scan {
        projects.set(".*:(service-a|service-b)")
    }
}

dependencies {
    implementation("org.octopusden.octopus-cloud-commons:octopus-security-common:2.0.15")
}
