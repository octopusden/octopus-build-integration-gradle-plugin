package org.octopusden.octopus.build.integration.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.octopusden.octopus.build.integration.gradle.plugin.extension.BuildIntegrationExtension
import org.octopusden.octopus.build.integration.gradle.plugin.service.DependenciesExtractor
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.COMPONENT_REGISTRY_URL_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.CONFIGURATIONS_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.OUTPUT_FILE_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.PROJECTS_PROPERTY
import org.octopusden.octopus.build.integration.gradle.plugin.task.ExportDependenciesTask.Companion.SCAN_ENABLED_PROPERTY
import org.octopusden.octopus.components.registry.client.ComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClientUrlProvider
import java.util.regex.PatternSyntaxException

class BuildIntegrationGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("buildIntegration", BuildIntegrationExtension::class.java)
        registerExportDependenciesTask(project, extension)
    }

    private fun registerExportDependenciesTask(project: Project, extension: BuildIntegrationExtension) {
        val dependenciesExtension = extension.dependenciesExtension

        val scanEnabledProvider = project.providers.gradleProperty(SCAN_ENABLED_PROPERTY).forUseAtConfigurationTime()
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
            val componentsRegistryClient = if (scanEnabledProvider.get() && componentsRegistryUrlProvider.get().isNotBlank()) {
                createComponentsRegistryClient(componentsRegistryUrlProvider.get())
            } else {
                null
            }
            val dependencies = DependenciesExtractor(componentsRegistryClient).getDependencies(
                project = project,
                manualComponents = dependenciesExtension.components.get(),
                scanEnabled = scanEnabledProvider.get(),
                projects = regexProcessing(projectsProvider.get()),
                configurations = regexProcessing(configurationsProvider.get())
            )
            outputFilePropertyProvider.orNull
                ?.let { task.outputFile.set(project.layout.buildDirectory.file(it)) }
                ?: task.outputFile.convention(dependenciesExtension.outputFile)
            task.dependencies.convention(dependencies)
        }
    }

    private fun createComponentsRegistryClient(componentsRegistryUrl: String): ComponentsRegistryServiceClient =
        ClassicComponentsRegistryServiceClient(
            object : ClassicComponentsRegistryServiceClientUrlProvider {
                override fun getApiUrl(): String = componentsRegistryUrl
            }
        )

    private fun regexProcessing(pattern: String): Regex {
        return try {
            pattern.toRegex()
        } catch (e: PatternSyntaxException) {
            throw GradleException("Invalid regex pattern: $pattern", e)
        }
    }

    companion object {
        const val EXPORT_DEPENDENCIES_TASK_NAME = "exportDependencies"
    }

}
