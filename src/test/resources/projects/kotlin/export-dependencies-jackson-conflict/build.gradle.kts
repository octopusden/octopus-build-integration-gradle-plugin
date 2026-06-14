buildscript {
    dependencies {
        classpath(platform("com.fasterxml.jackson:jackson-bom:${project.properties["jackson.version"]}"))
        classpath("com.fasterxml.jackson.core:jackson-databind")
        classpath("com.fasterxml.jackson.core:jackson-core")
        classpath("com.fasterxml.jackson.core:jackson-annotations")
    }
}

plugins {
    id("org.octopusden.octopus-build-integration")
    id("org.jetbrains.kotlin.jvm") version (project.properties["kotlin.version"] as String)
}

releaseManagement {
    releaseDependencies {
        component("component_a", "1.0.0")
        component("component_b:1.1.0")
    }
}

dependencies {
    implementation(platform("com.fasterxml.jackson:jackson-bom:${project.properties["jackson.version"]}"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.octopusden.octopus.releng:versions-api:2.0.10")
}
