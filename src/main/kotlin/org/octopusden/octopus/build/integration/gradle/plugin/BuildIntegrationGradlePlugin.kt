package org.octopusden.octopus.build.integration.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.octopusden.octopus.build.integration.gradle.plugin.extension.ReleaseManagementExtension
import org.octopusden.octopus.build.integration.gradle.plugin.service.DependenciesExtractor
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask

class BuildIntegrationGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("releaseManagement", ReleaseManagementExtension::class.java)
        val scan = extension.scan

        val scanEnabledProvider = project.providers
            .gradleProperty(SCAN_ENABLED_PROPERTY)
            .map { it.toBoolean() }
            .orElse(scan.enabled)

        val componentsRegistryUrlProvider = project.providers
            .environmentVariable(COMPONENT_REGISTRY_SERVICE_URL_ENV)
            .orElse(scan.componentsRegistryUrl)

        val projectsProvider = project.providers
            .gradleProperty(PROJECTS_PROPERTY)
            .orElse(scan.projects)

        val configurationsProvider = project.providers
            .gradleProperty(CONFIGURATIONS_PROPERTY)
            .orElse(scan.configurations)

        val outputFilePropertyProvider = project.providers.gradleProperty(OUTPUT_FILE_PROPERTY)

        project.tasks.register(EXPORT_DEPENDENCIES_TASK_NAME, ExportDependenciesTask::class.java) { task ->
            task.outputFile.set(
                outputFilePropertyProvider.orNull?.let { project.layout.buildDirectory.file(it) }
                    ?: extension.outputFile,
            )
            task.dependencies.set(
                if (scanEnabledProvider.get()) {
                    val componentsRegistryUrl = componentsRegistryUrlProvider.orNull
                    if (componentsRegistryUrl.isNullOrBlank()) {
                        throw GradleException("'scan' is enabled, but 'componentsRegistryUrl' is not configured!")
                    }
                    // Result may include components with the same name but different versions.
                    // Resolving such conflicts will be performed at later stages.
                    DependenciesExtractor(
                        project = project,
                        componentsRegistryUrl = componentsRegistryUrl,
                        projectsPattern = projectsProvider.get(),
                        configurationsPattern = configurationsProvider.get(),
                    ).extract() + extension.releaseDependencies.components.get()
                } else {
                    extension.releaseDependencies.components.get()
                },
            )
        }
    }

    companion object {
        const val EXPORT_DEPENDENCIES_TASK_NAME = "exportDependencies"

        const val COMPONENT_REGISTRY_SERVICE_URL_ENV = "COMPONENT_REGISTRY_SERVICE_URL"

        const val SCAN_ENABLED_PROPERTY = "dependencies.scan.enabled"
        const val PROJECTS_PROPERTY = "dependencies.scan.projects"
        const val CONFIGURATIONS_PROPERTY = "dependencies.scan.configurations"
        const val OUTPUT_FILE_PROPERTY = "dependencies.outputFile"
    }
}
