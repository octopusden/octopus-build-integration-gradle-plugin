package org.octopusden.octopus.build.integration.gradle.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.octopusden.octopus.build.integration.gradle.plugin.runner.gradleProcessInstance
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesToTeamcity.Companion.COMPONENT_REGISTRY_URL_PROPERTY
import java.util.stream.Stream

class ExportDependenciesToTeamcityTest {

    @ParameterizedTest
    @MethodSource("gradleJavaParameters")
    fun testOnlyExplicitComponents(gradleVersion: String, javaHome: String) {
        val (instance, _) = gradleProcessInstance {
            projectPath = "projects/export-dependencies-only-explicit-components"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf("-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost")
            additionalEnvVariables = mapOf(
                "JAVA_HOME" to javaHome
            )
        }
        assertEquals(0, instance.exitCode)
        val out = instance.stdOut.joinToString("\n")
        assertTrue(out.contains("##teamcity[setParameter name='DEPENDENCIES' value='a:1.0.0,b:1.1.0']"))
    }

    @ParameterizedTest
    @MethodSource("gradleJavaParameters")
    fun testProjectsFilter(gradleVersion: String, javaHome: String) {
        val (instance, _) = gradleProcessInstance {
            projectPath = "projects/export-dependencies-projects-filter"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf("-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost")
            additionalEnvVariables = mapOf(
                "JAVA_HOME" to javaHome
            )
        }
        assertEquals(0, instance.exitCode)
        val out = instance.stdOut.joinToString("\n")
        assertTrue(out.contains("##teamcity[setParameter name='DEPENDENCIES' value='a:1.0.0,b:1.1.0,components-registry-service-client:2.0.62,versions-api:2.0.10']"))
    }

    @ParameterizedTest
    @MethodSource("gradleJavaParameters")
    fun testScanDefaultValues(gradleVersion: String, javaHome: String) {
        val (instance, _) = gradleProcessInstance {
            projectPath = "projects/export-dependencies-scan-default-values"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf("-P$COMPONENT_REGISTRY_URL_PROPERTY=http://$componentsRegistryHost")
            additionalEnvVariables = mapOf(
                "JAVA_HOME" to javaHome
            )
        }
        assertEquals(0, instance.exitCode)
        val out = instance.stdOut.joinToString("\n")
        assertTrue(out.contains("##teamcity[setParameter name='DEPENDENCIES' value='a:1.0.0,b:1.1.0,components-registry-service-client:2.0.62,octopus-security-common:2.0.15,versions-api:2.0.10']"))
    }

    companion object {
        private val EXPORT_DEPENDENCIES_COMMAND = arrayOf("exportDependenciesToTeamcity", "--info", "--stacktrace")
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