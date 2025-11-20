import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin")
}

group = "org.octopusden.octopus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.octopusden.octopus.infrastructure:components-registry-service-client:${properties["octopus-components-registry.version"]}")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.8.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.6")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_21
}

gradlePlugin {
    plugins {
        create("buildIntegration") {
            id = "org.octopusden.octopus-build-integration"
            displayName = "Octopus Build Integration Gradle Plugin"
            description = "Gradle plugin for Octopus build integration"
            implementationClass = "org.octopusden.octopus.build.integration.gradle.plugin.BuildIntegrationGradlePlugin"
        }
    }
}

