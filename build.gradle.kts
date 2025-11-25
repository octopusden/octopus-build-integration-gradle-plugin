import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.Duration

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin")
    id("com.jfrog.artifactory")
    id("org.octopusden.octopus.oc-template")
}

group = "org.octopusden.octopus"

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

java {
    withSourcesJar()
    withJavadocJar()
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_1_8
}

ext {
    System.getenv().let {
        set("signingRequired", it.containsKey("ORG_GRADLE_PROJECT_signingKey") && it.containsKey("ORG_GRADLE_PROJECT_signingPassword"))
        set("dockerRegistry", it.getOrDefault("DOCKER_REGISTRY", properties["docker.registry"]))
        set("octopusGithubDockerRegistry", it.getOrDefault("OCTOPUS_GITHUB_DOCKER_REGISTRY", project.properties["octopus.github.docker.registry"]))
        set("okdActiveDeadlineSeconds", it.getOrDefault("OKD_ACTIVE_DEADLINE_SECONDS", properties["okd.active-deadline-seconds"]))
        set("okdProject", it.getOrDefault("OKD_PROJECT", properties["okd.project"]))
        set("okdClusterDomain", it.getOrDefault("OKD_CLUSTER_DOMAIN", properties["okd.cluster-domain"]))
        set("okdWebConsoleUrl", (it.getOrDefault("OKD_WEB_CONSOLE_URL", properties["okd.web-console-url"]) as String).trimEnd('/'))
    }
}
val mandatoryProperties = mutableListOf("dockerRegistry", "octopusGithubDockerRegistry", "okdActiveDeadlineSeconds", "okdProject", "okdClusterDomain")
val undefinedProperties = mandatoryProperties.filter { (project.ext[it] as String).isBlank() }
if (undefinedProperties.isNotEmpty()) {
    throw IllegalArgumentException(
        "Start gradle build with" +
                (if (undefinedProperties.contains("dockerRegistry")) " -Pdocker.registry=..." else "") +
                (if (undefinedProperties.contains("octopusGithubDockerRegistry")) " -Poctopus.github.docker.registry=..." else "") +
                (if (undefinedProperties.contains("okdActiveDeadlineSeconds")) " -Pokd.active-deadline-seconds=..." else "") +
                (if (undefinedProperties.contains("okdProject")) " -Pokd.project=..." else "") +
                (if (undefinedProperties.contains("okdClusterDomain")) " -Pokd.cluster-domain=..." else "") +
                " or set env variable(s):" +
                (if (undefinedProperties.contains("dockerRegistry")) " DOCKER_REGISTRY" else "") +
                (if (undefinedProperties.contains("octopusGithubDockerRegistry")) " OCTOPUS_GITHUB_DOCKER_REGISTRY" else "") +
                (if (undefinedProperties.contains("okdActiveDeadlineSeconds")) " OKD_ACTIVE_DEADLINE_SECONDS" else "") +
                (if (undefinedProperties.contains("okdProject")) " OKD_PROJECT" else "") +
                (if (undefinedProperties.contains("okdClusterDomain")) " OKD_CLUSTER_DOMAIN" else "")
    )
}
fun String.getExt() = project.ext[this].toString()

val commonOkdParameters = mapOf(
    "ACTIVE_DEADLINE_SECONDS" to "okdActiveDeadlineSeconds".getExt(),
    "DOCKER_REGISTRY" to "dockerRegistry".getExt()
)

val testParameters by lazy {
    mapOf(
        "octopus-build-integration.version" to project.version,
        "test.components-registry-host" to ocTemplate.getOkdHost("comp-reg")
    )
}

tasks.test {
    useJUnitPlatform()
    dependsOn("publishToMavenLocal")
    ocTemplate.isRequiredBy(this)
    doFirst {
        testParameters.forEach { systemProperty(it.key, it.value) }
    }
}

ocTemplate {
    workDir.set(layout.buildDirectory.dir("okd"))
    clusterDomain.set("okdClusterDomain".getExt())
    namespace.set("okdProject".getExt())
    prefix.set("build-integr")

    "okdWebConsoleUrl".getExt().takeIf { it.isNotBlank() }?.let {
        webConsoleUrl.set(it)
    }

    service("comp-reg") {
        templateFile.set(rootProject.layout.projectDirectory.file("okd/template/components-registry.yaml"))
        val componentsRegistryWorkDir = layout.projectDirectory.dir("src/test/resources/components-registry-data").asFile.absolutePath
        parameters.set(commonOkdParameters + mapOf(
            "COMPONENTS_REGISTRY_SERVICE_VERSION" to properties["octopus-components-registry-service.version"] as String,
            "AGGREGATOR_GROOVY_CONTENT" to file("${componentsRegistryWorkDir}/Aggregator.groovy").readText(),
            "DEFAULTS_GROOVY_CONTENT" to file("${componentsRegistryWorkDir}/Defaults.groovy").readText(),
            "TEST_COMPONENTS_GROOVY_CONTENT" to file("${componentsRegistryWorkDir}/TestComponents.groovy").readText(),
            "APPLICATION_DEV_CONTENT" to layout.projectDirectory.dir("okd/config/components-registry-service.yaml").asFile.readText()
        ))
    }

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

artifactory {
    publish {
        val baseUrl = System.getenv().getOrDefault("ARTIFACTORY_URL", project.properties["artifactoryUrl"])
        if (baseUrl != null) {
            contextUrl = "$baseUrl/artifactory"
        }
        repository {
            repoKey = "rnd-maven-dev-local"
            username = System.getenv().getOrDefault("ARTIFACTORY_DEPLOYER_USERNAME", project.properties["NEXUS_USER"]).toString()
            password = System.getenv().getOrDefault("ARTIFACTORY_DEPLOYER_PASSWORD", project.properties["NEXUS_PASSWORD"]).toString()
        }
        defaults {
            publications("ALL_PUBLICATIONS")
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("MAVEN_USERNAME"))
            password.set(System.getenv("MAVEN_PASSWORD"))
        }
    }
    transitionCheckOptions {
        maxRetries.set(60)
        delayBetween.set(Duration.ofSeconds(30))
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set(project.name)
                description.set(project.description)
                url = "https://github.com/octopusden/octopus-build-integration-gradle-plugin.git"
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/octopusden/octopus-build-integration-gradle-plugin.git")
                    connection.set("scm:git://github.com/octopusden/octopus-build-integration-gradle-plugin.git")
                }
                developers {
                    developer {
                        id.set("octopus")
                        name.set("octopus")
                    }
                }
            }
        }
    }
}

