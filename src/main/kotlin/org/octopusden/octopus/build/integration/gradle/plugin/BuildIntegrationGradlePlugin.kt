package org.octopusden.octopus.build.integration.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.octopusden.octopus.build.integration.gradle.plugin.extension.BuildIntegrationExtension
import org.octopusden.octopus.build.integration.gradle.plugin.service.DependenciesExtractor
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask

class BuildIntegrationGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("buildIntegration", BuildIntegrationExtension::class.java)
        val dependenciesExtension = extension.dependenciesExtension

        val scanEnabledProvider = project.providers.gradleProperty(SCAN_ENABLED_PROPERTY)
            .map { it.toBoolean() }
            .orElse(dependenciesExtension.scan.enabled)

        val componentsRegistryUrlProvider = project.providers.gradleProperty(COMPONENT_REGISTRY_URL_PROPERTY)
            .orElse(dependenciesExtension.scan.componentsRegistryUrl)

        val projectsProvider = project.providers.gradleProperty(PROJECTS_PROPERTY)
            .orElse(dependenciesExtension.scan.projects)

        val configurationsProvider = project.providers.gradleProperty(CONFIGURATIONS_PROPERTY)
            .orElse(dependenciesExtension.scan.configurations)

        val outputFilePropertyProvider = project.providers.gradleProperty(OUTPUT_FILE_PROPERTY)

        project.tasks.register(EXPORT_DEPENDENCIES_TASK_NAME, ExportDependenciesTask::class.java) { task ->
            task.outputFile.set(
                outputFilePropertyProvider.orNull?.let { project.layout.buildDirectory.file(it) }
                    ?: dependenciesExtension.outputFile
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
                        configurationsPattern = configurationsProvider.get()
                    ).extract() + dependenciesExtension.components.get()
                } else {
                    dependenciesExtension.components.get()
                }
            )
        }
    }

    companion object {
        const val EXPORT_DEPENDENCIES_TASK_NAME = "exportDependencies"

        const val SCAN_ENABLED_PROPERTY = "buildIntegration.dependencies.scan.enabled"
        const val COMPONENT_REGISTRY_URL_PROPERTY = "buildIntegration.dependencies.scan.componentsRegistryUrl"
        const val PROJECTS_PROPERTY = "buildIntegration.dependencies.scan.projects"
        const val CONFIGURATIONS_PROPERTY = "buildIntegration.dependencies.scan.configurations"
        const val OUTPUT_FILE_PROPERTY = "buildIntegration.dependencies.outputFile"
    }

}
