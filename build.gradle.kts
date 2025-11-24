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
    implementation("org.octopusden.octopus.infrastructure:components-registry-service-client:${properties["octopus-components-registry-service.version"]}")
    implementation("com.fasterxml.jackson.core:jackson-annotations:${properties["jackson.version"]}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${properties["jackson.version"]}")
    testApi("com.platformlib:platformlib-process-local:${properties["platformlib-process.version"]}")
    testImplementation("org.assertj:assertj-core:${project.extra["assertj.version"]}")
    testImplementation(platform("org.junit:junit-bom:${project.extra["junit-jupiter.version"]}"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${properties["mockito-kotlin.version"]}")
}

val testParameters by lazy {
    mapOf(
        "octopus-build-integration.version" to project.version
    )
}

tasks.test {
    useJUnitPlatform()
    dependsOn("publishToMavenLocal")
    doFirst {
        testParameters.forEach { systemProperty(it.key, it.value) }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_1_8
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

