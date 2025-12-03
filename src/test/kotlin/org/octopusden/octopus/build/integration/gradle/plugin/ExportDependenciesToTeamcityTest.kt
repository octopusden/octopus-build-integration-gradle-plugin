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
import org.octopusden.octopus.build.integration.gradle.plugin.extension.DependenciesExtension.Component
import org.octopusden.octopus.build.integration.gradle.plugin.runner.gradleProcessInstance
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.COMPONENT_REGISTRY_URL_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.CONFIGURATIONS_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.OUTPUT_FILE_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.PROJECTS_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.SCAN_ENABLED_PROPERTY
import java.util.stream.Stream

class ExportDependenciesToTeamcityTest {

    private val mapper = ObjectMapper().apply { enable(SerializationFeature.INDENT_OUTPUT) }

    @ParameterizedTest
    @MethodSource("testParameters")
    fun testOnlyExplicitComponents(gradleVersion: String, javaHome: String, dsl: String) {
        val (instance, projectPath) = gradleProcessInstance {
            projectPath = "projects/$dsl/export-dependencies-only-explicit-components"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf(
                "-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost",
                "-P$SCAN_ENABLED_PROPERTY=false"
            )
            additionalEnvVariables = mapOf("JAVA_HOME" to javaHome)
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
    @MethodSource("testParameters")
    fun testProjectsFilter(gradleVersion: String, javaHome: String, dsl: String) {
        val (instance, projectPath) = gradleProcessInstance {
            projectPath = "projects/$dsl/export-dependencies-projects-filter"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf(
                "-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost",
                "-P$SCAN_ENABLED_PROPERTY=true"
            )
            additionalEnvVariables = mapOf("JAVA_HOME" to javaHome)
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
    @MethodSource("testParameters")
    fun testScanDefaultValues(gradleVersion: String, javaHome: String, dsl: String) {
        val (instance, projectPath) = gradleProcessInstance {
            projectPath = "projects/$dsl/export-dependencies-scan-default-values"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf(
                "-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost",
                "-P$SCAN_ENABLED_PROPERTY=true"
            )
            additionalEnvVariables = mapOf("JAVA_HOME" to javaHome)
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

    @ParameterizedTest
    @MethodSource("testParameters")
    fun testOverrideParameters(gradleVersion: String, javaHome: String, dsl: String) {
        val outputFile = "test-output-file.json"
        val (instance, projectPath) = gradleProcessInstance {
            projectPath = "projects/$dsl/export-dependencies-override-parameters"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf(
                "-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost",
                "-P$SCAN_ENABLED_PROPERTY=true",
                "-P$PROJECTS_PROPERTY=octopus.+",
                "-P$CONFIGURATIONS_PROPERTY=runtime.+|compile.+",
                "-P$OUTPUT_FILE_PROPERTY=$outputFile"
            )
            additionalEnvVariables = mapOf("JAVA_HOME" to javaHome)
        }
        assertEquals(0, instance.exitCode)
        val file = projectPath.resolve("build/$outputFile").toFile()
        assertTrue(file.exists(), "Dependencies file was not created")
        val result = listOf(
            Component("component_a", "1.0.0"),
            Component("component_b", "1.1.0"),
            Component("octopus-security-common", "2.0.15")
        )
        assertEquals(mapper.writeValueAsString(result), file.readText())
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    fun testConfigurationCacheReuse(gradleVersion: String, javaHome: String, dsl: String) {
        val outputFile = "cc-output-$dsl-$gradleVersion.json"
        val (firstRun, tmpDir) = gradleProcessInstance {
            projectPath = "projects/$dsl/export-dependencies-scan-default-values"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf(
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost",
                "-P$SCAN_ENABLED_PROPERTY=true",
                "-P$OUTPUT_FILE_PROPERTY=$outputFile"
            )
            additionalEnvVariables = mapOf("JAVA_HOME" to javaHome)
        }
        assertEquals(0, firstRun.exitCode)
        val (secondRun, _) = gradleProcessInstance {
            projectPath = tmpDir.toString()
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf(
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost",
                "-P$SCAN_ENABLED_PROPERTY=true",
                "-P$OUTPUT_FILE_PROPERTY=$outputFile"
            )
            additionalEnvVariables = mapOf("JAVA_HOME" to javaHome)
            reuseExistingDir = true
        }
        assertEquals(0, secondRun.exitCode)

        val fullOutput = secondRun.stdOut + "\n" + secondRun.stdErr
        assertTrue(fullOutput.contains("Configuration cache entry reused"))

        val resultFile = tmpDir.resolve("build/$outputFile").toFile()
        assertTrue(resultFile.exists())
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
        fun testParameters(): Stream<Arguments> {
            val dsls = listOf("kotlin", "groovy")
            val gradleJava = listOf(
                "6" to java8,
                "7" to java8,
                "8" to java8,
                "9" to java17
            )
            val params = dsls.flatMap { dsl ->
                gradleJava.map { (gradleVersion, javaHome) ->
                    Arguments.of(gradleVersion, javaHome, dsl)
                }
            }
            return params.stream()
        }
    }

}