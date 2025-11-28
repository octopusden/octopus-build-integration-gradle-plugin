package org.octopusden.octopus.build.integration.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.octopusden.octopus.build.integration.gradle.plugin.extension.BuildIntegrationExtension
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependencies
import org.slf4j.LoggerFactory

class BuildIntegrationGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val rootProject = project.rootProject
        rootProject.extensions.findByType(BuildIntegrationExtension::class.java)
            ?: rootProject.extensions.create("buildIntegration", BuildIntegrationExtension::class.java)
        registerExportDependenciesToTeamcityTask(rootProject)
    }

    private fun registerExportDependenciesToTeamcityTask(rootProject: Project): TaskProvider<ExportDependencies> {
        val task = rootProject.tasks.findByName(EXPORT_DEPENDENCIES_TASK_NAME)
        return if (task != null) {
            rootProject.tasks.named(EXPORT_DEPENDENCIES_TASK_NAME, ExportDependencies::class.java)
        } else {
            rootProject.tasks.register(EXPORT_DEPENDENCIES_TASK_NAME, ExportDependencies::class.java)
                .also { logger.info("Registered task {}", EXPORT_DEPENDENCIES_TASK_NAME) }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildIntegrationGradlePlugin::class.java)
        const val EXPORT_DEPENDENCIES_TASK_NAME = "exportDependencies"
    }
}
