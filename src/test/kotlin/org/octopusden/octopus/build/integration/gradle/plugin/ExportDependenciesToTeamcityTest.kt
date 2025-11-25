package org.octopusden.octopus.build.integration.gradle.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.octopusden.octopus.build.integration.gradle.plugin.runner.gradleProcessInstance
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesToTeamcity.Companion.COMPONENT_REGISTRY_SERVICE_URL_PROPERTY

class ExportDependenciesToTeamcityTest {

    @ParameterizedTest
    @MethodSource("gradleVersions")
    fun testExportComponents(gradleVersion: String) {
        val (instance, _) = gradleProcessInstance {
            projectPath = "projects/export-dependencies-components"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf("-P$COMPONENT_REGISTRY_SERVICE_URL_PROPERTY=http://$componentsRegistryHost")
        }
        assertEquals(0, instance.exitCode)
        val out = instance.stdOut.joinToString("\n")
        assertTrue(out.contains("##teamcity[setParameter name='DEPENDENCIES' value='a:1.0.0,b:1.1.0']"))
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    fun testIncludeAllDependencies(gradleVersion: String) {
        val (instance, _) = gradleProcessInstance {
            projectPath = "projects/export-dependencies-include-all"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf("-P$COMPONENT_REGISTRY_SERVICE_URL_PROPERTY=http://$componentsRegistryHost")
        }
        assertEquals(0, instance.exitCode)
        val out = instance.stdOut.joinToString("\n")
        assertTrue(out.contains("##teamcity[setParameter name='DEPENDENCIES' value='a:1.0.0,b:1.1.0,components-registry-service-client:2.0.62,octopus-security-common:2.0.15']"))
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    fun testExcludeFilter(gradleVersion: String) {
        val (instance, _) = gradleProcessInstance {
            projectPath = "projects/export-dependencies-exclude-filter"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf("-P$COMPONENT_REGISTRY_SERVICE_URL_PROPERTY=http://$componentsRegistryHost")
        }
        assertEquals(0, instance.exitCode)
        val out = instance.stdOut.joinToString("\n")
        assertTrue(out.contains("##teamcity[setParameter name='DEPENDENCIES' value='a:1.0.0,b:1.1.0']"))
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    fun testSingleInclude(gradleVersion: String) {
        val (instance, _) = gradleProcessInstance {
            projectPath = "projects/export-dependencies-single-include"
            gradleWrapperPath = "wrappers/gradle-$gradleVersion"
            tasks = EXPORT_DEPENDENCIES_COMMAND
            additionalArguments = arrayOf("-P$COMPONENT_REGISTRY_SERVICE_URL_PROPERTY=http://$componentsRegistryHost")
        }
        assertEquals(0, instance.exitCode)
        val out = instance.stdOut.joinToString("\n")
        assertTrue(out.contains("##teamcity[setParameter name='DEPENDENCIES' value='a:1.0.0,b:1.1.0,octopus-security-common:2.0.15']"))
    }

    companion object {
        private val EXPORT_DEPENDENCIES_COMMAND = arrayOf("exportDependenciesToTeamcity", "--info", "--stacktrace")
        private val componentsRegistryHost = System.getProperty("test.components-registry-host")
            ?: throw Exception("System property 'test.components-registry-host' must be defined")

        @JvmStatic
        fun gradleVersions() = listOf("6", "7", "8", "9")
    }

}