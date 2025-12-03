package org.octopusden.octopus.build.integration.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.octopusden.octopus.build.integration.gradle.plugin.extension.BuildIntegrationExtension
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.COMPONENT_REGISTRY_URL_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.CONFIGURATIONS_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.OUTPUT_FILE_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.PROJECTS_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.SCAN_ENABLED_PROPERTY

class BuildIntegrationGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("buildIntegration", BuildIntegrationExtension::class.java)
        registerExportDependenciesTask(project, extension)
    }

    private fun registerExportDependenciesTask(project: Project, extension: BuildIntegrationExtension) {
        project.tasks.register(EXPORT_DEPENDENCIES_TASK_NAME, ExportDependenciesTask::class.java) { task ->
            val dependenciesExtension = extension.dependenciesExtension

            task.manualComponents.convention(dependenciesExtension.components)

            project.findProperty(SCAN_ENABLED_PROPERTY)?.toString()?.toBoolean()
                ?.let { task.scanEnabled.set(it) }
                ?: task.scanEnabled.convention(dependenciesExtension.scan.enabled)

            project.findProperty(COMPONENT_REGISTRY_URL_PROPERTY)?.toString()
                ?.let { task.componentsRegistryUrl.set(it) }
                ?: task.componentsRegistryUrl.convention(dependenciesExtension.scan.componentsRegistryUrl)

            project.findProperty(PROJECTS_PROPERTY)?.toString()
                ?.let { task.projects.set(it) }
                ?: task.projects.convention(dependenciesExtension.scan.projects)

            project.findProperty(CONFIGURATIONS_PROPERTY)?.toString()
                ?.let { task.configurations.set(it) }
                ?: task.configurations.convention(dependenciesExtension.scan.configurations)

            project.findProperty(OUTPUT_FILE_PROPERTY)?.toString()
                ?.let { rel ->
                    task.outputFile.set(
                        project.layout.buildDirectory.file(rel)
                    )
                }
                ?: task.outputFile.convention(dependenciesExtension.outputFile)
        }
    }

    companion object {
        const val EXPORT_DEPENDENCIES_TASK_NAME = "exportDependencies"
    }

}
