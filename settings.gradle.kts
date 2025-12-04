import java.net.InetAddress
import java.util.zip.CRC32

pluginManagement {
    plugins {
        kotlin("jvm") version extra["kotlin.version"] as String
        id("io.github.gradle-nexus.publish-plugin") version extra["nexus-plugin.version"] as String
        id("org.octopusden.octopus.oc-template") version extra["octopus-oc-template.version"] as String
        id("com.jfrog.artifactory") version extra["com-jfrog-artifactory.version"] as String
        id("org.asciidoctor.jvm.convert") version extra["asciidoctor-jvm-convert.version"] as String
        id("com.platformlib.gradle-wrapper") version extra["platformlib-gradle-wrapper.version"] as String
    }
}
rootProject.name = "octopus-build-integration-gradle-plugin"

val defaultVersion = with(CRC32()) {
    update(InetAddress.getLocalHost().hostName.toByteArray())
    value
}.toString() + "-SNAPSHOT"
gradle.beforeProject { project.version = gradle.startParameter.projectProperties["version"] ?: defaultVersion }