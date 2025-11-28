package org.octopusden.octopus.build.integration.gradle.plugin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.octopusden.octopus.build.integration.gradle.plugin.BuildIntegrationGradlePlugin.Companion.EXPORT_DEPENDENCIES_TASK_NAME
import org.octopusden.octopus.build.integration.gradle.plugin.extension.DependenciesExtension.Companion.DEFAULT_OUTPUT_FILE
import org.octopusden.octopus.build.integration.gradle.plugin.model.Component
import org.octopusden.octopus.build.integration.gradle.plugin.runner.gradleProcessInstance
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependencies.Companion.COMPONENT_REGISTRY_URL_PROPERTY
import java.util.stream.Stream

class ExportDependenciesToTeamcityTest {

    private val mapper = ObjectMapper().apply { enable(SerializationFeature.INDENT_OUTPUT) }

    @ParameterizedTest
    @MethodSource("gradleJavaParameters")
    fun testOnlyExplicitComponents(gradleVersion: String, javaHome: String) {
        val (instance, projectPath) = gradleProcessInstance {
            projectPath = "projects/export-dependencies-only-explicit-components"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf("-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost")
            additionalEnvVariables = mapOf(
                "JAVA_HOME" to javaHome
            )
        }
        assertEquals(0, instance.exitCode)
        val file = projectPath.resolve("build/$DEFAULT_OUTPUT_FILE").toFile()
        assertTrue(file.exists(), "Dependencies file was not created")
        val result = listOf(
            Component("component_a", "1.0.0"),
            Component("component_b", "1.1.0"),
        )
        assertEquals(mapper.writeValueAsString(result), file.readText())
    }

    @ParameterizedTest
    @MethodSource("gradleJavaParameters")
    fun testProjectsFilter(gradleVersion: String, javaHome: String) {
        val (instance, projectPath) = gradleProcessInstance {
            projectPath = "projects/export-dependencies-projects-filter"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf("-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost")
            additionalEnvVariables = mapOf(
                "JAVA_HOME" to javaHome
            )
        }
        assertEquals(0, instance.exitCode)
        val file = projectPath.resolve("build/$DEFAULT_OUTPUT_FILE").toFile()
        assertTrue(file.exists(), "Dependencies file was not created")
        val result = listOf(
            Component("component_a", "1.0.0"),
            Component("component_b", "1.1.0"),
            Component("components-registry-service-client", "2.0.62"),
            Component("versions-api", "2.0.10")
        )
        assertEquals(mapper.writeValueAsString(result), file.readText())
    }

    @ParameterizedTest
    @MethodSource("gradleJavaParameters")
    fun testScanDefaultValues(gradleVersion: String, javaHome: String) {
        val (instance, projectPath) = gradleProcessInstance {
            projectPath = "projects/export-dependencies-scan-default-values"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf("-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost", )
            additionalEnvVariables = mapOf(
                "JAVA_HOME" to javaHome
            )
        }
        assertEquals(0, instance.exitCode)
        val file = projectPath.resolve("build/$DEFAULT_OUTPUT_FILE").toFile()
        assertTrue(file.exists(), "Dependencies file was not created")
        val result = listOf(
            Component("component_a", "1.0.0"),
            Component("component_b", "1.1.0"),
            Component("components-registry-service-client", "2.0.62"),
            Component("octopus-security-common", "2.0.15"),
            Component("versions-api", "2.0.10")
        )
        assertEquals(mapper.writeValueAsString(result), file.readText())
    }

    companion object {
        private val EXPORT_DEPENDENCIES_COMMAND = arrayOf(EXPORT_DEPENDENCIES_TASK_NAME, "--info", "--stacktrace")
        private val componentsRegistryHost = System.getProperty("test.components-registry-host")
            ?: throw Exception("System property 'test.components-registry-host' must be defined")
        private val java8 = System.getProperty("test.java8-home")
            ?: throw Exception("System property 'test.java8-home' must be defined")
        private val java17 = System.getProperty("test.java17-home")
            ?: throw Exception("System property 'test.java17-home' must be defined")

        @JvmStatic
        fun gradleJavaParameters(): Stream<Arguments> {
            val params = listOf(
                Arguments.of("6", java8),
                Arguments.of("7", java8),
                Arguments.of("8", java8),
                Arguments.of("9", java17)
            )
            return params.stream()
        }
    }

}