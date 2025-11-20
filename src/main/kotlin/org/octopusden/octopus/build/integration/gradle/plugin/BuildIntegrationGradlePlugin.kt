package org.octopusden.octopus.build.integration.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.octopusden.octopus.build.integration.gradle.plugin.extension.BuildIntegrationExtension
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesToTeamcity
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesToTeamcity.Companion.INCLUDE_ALL_DEPENDENCIES_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesToTeamcity.Companion.booleanPropertyOrDefault
import org.slf4j.LoggerFactory

class BuildIntegrationGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val rootProject = project.rootProject
        val extension = rootProject.extensions.findByType(BuildIntegrationExtension::class.java)
            ?: rootProject.extensions.create(
                "buildIntegration",
                BuildIntegrationExtension::class.java
            )
        configureTask(rootProject, extension)
    }

    private fun configureTask(rootProject: Project, extension: BuildIntegrationExtension) {
        logger.info("BuildIntegrationPlugin applied to project {}", rootProject.path)
        val exportTaskProvider: TaskProvider<ExportDependenciesToTeamcity> =
            if (rootProject.tasks.findByName(EXPORT_DEPENDENCIES_TASK_NAME) != null) {
                rootProject.tasks.named(EXPORT_DEPENDENCIES_TASK_NAME, ExportDependenciesToTeamcity::class.java)
            } else {
                rootProject.tasks.register(EXPORT_DEPENDENCIES_TASK_NAME, ExportDependenciesToTeamcity::class.java)
                    .also {
                        logger.info("Registered task {}", EXPORT_DEPENDENCIES_TASK_NAME)
                    }
            }
        val startParameter = rootProject.gradle.startParameter
        val exportExplicitlyRequested = startParameter.taskNames.any {
            it.endsWith(EXPORT_DEPENDENCIES_TASK_NAME)
        }
        if (exportExplicitlyRequested) {
            logger.debug(
                "Skip auto export: task {} is explicitly requested in the build",
                EXPORT_DEPENDENCIES_TASK_NAME
            )
            return
        }
        if (startParameter.isOffline) {
            logger.debug("Skip auto export: Gradle is running in offline mode")
            return
        }
        rootProject.gradle.buildFinished { result ->
            if (result.failure != null) {
                logger.debug("Skip auto export: build finished with failure ({})", result.failure?.message)
                return@buildFinished
            }
            val config = extension.buildConfig()
            val includeAllDependencies = rootProject.booleanPropertyOrDefault(
                config.gradleDependenciesSelector.includeAllDependencies,
                INCLUDE_ALL_DEPENDENCIES_PROPERTY
            )
            if (!config.isConfigured && !includeAllDependencies) {
                logger.debug("Skip auto export: buildIntegration.exportDependencies is not configured and includeAllDependencies is not set")
                return@buildFinished
            }
            val hasBuildVersion = rootProject.hasProperty("buildVersion")
            if (!config.autoRegistration && !hasBuildVersion) {
                logger.debug("Skip auto export: autoRegistration is disabled and 'buildVersion' property is not set")
                return@buildFinished
            }
            logger.info("Running {} automatically after successful build", EXPORT_DEPENDENCIES_TASK_NAME)
            val task = exportTaskProvider.get()
            task.exportDependencies()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildIntegrationGradlePlugin::class.java)
        private const val EXPORT_DEPENDENCIES_TASK_NAME = "exportDependenciesToTeamcity"
    }
}
