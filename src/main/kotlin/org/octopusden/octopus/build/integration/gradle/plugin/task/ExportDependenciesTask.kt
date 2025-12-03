package org.octopusden.octopus.build.integration.gradle.plugin.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.octopusden.octopus.build.integration.gradle.plugin.service.DependenciesExtractor
import org.octopusden.octopus.build.integration.gradle.plugin.extension.DependenciesExtension.Component
import org.octopusden.octopus.components.registry.client.ComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClientUrlProvider
import java.util.regex.PatternSyntaxException

abstract class ExportDependenciesTask : DefaultTask() {

    @get:Input
    abstract val scanEnabled: Property<Boolean>

    @get:Input
    abstract val componentsRegistryUrl: Property<String>

    @get:Input
    abstract val projects: Property<String>

    @get:Input
    abstract val configurations: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val manualComponents: SetProperty<Component>

    @TaskAction
    fun exportDependencies() {
        logger.info(
            "ExportDependenciesTask started. scanEnabled={}, componentsRegistryUrl={}, projects={}, configurations={}, outputFile={}",
            scanEnabled.get(), componentsRegistryUrl.get(), projects.get(), configurations.get(), outputFile.get()
        )
        val componentsRegistryClient = if (scanEnabled.get()) {
            if (componentsRegistryUrl.get().isBlank()) {
                throw GradleException("$SCAN_ENABLED_PROPERTY=true, but componentsRegistryUrl is null or empty")
            }
            val client = createComponentsRegistryClient(componentsRegistryUrl.get())
            logger.info("ExportDependenciesTask: supported group ids: {}", client.getSupportedGroupIds())
            client
        } else {
            logger.info("ExportDependenciesTask: scan disabled, components registry client will not be created")
            null
        }
        val exportService = DependenciesExtractor(componentsRegistryClient)
        val dependencies = exportService.getDependencies(
            project = project,
            manualComponents = manualComponents.get(),
            scanEnabled = scanEnabled.get(),
            projects = regexProcessing(projects.get()),
            configurations = regexProcessing(configurations.get())
        )
        val jsonResult = ObjectMapper().apply { enable(SerializationFeature.INDENT_OUTPUT) }.writeValueAsString(dependencies)
        logger.info("ExportDependenciesTask: resulting dependencies: {}", dependencies)
        outputFile.get().asFile.parentFile.mkdirs()
        outputFile.get().asFile.writeText(jsonResult)
        logger.info("ExportDependenciesTask: exported dependencies written to: ${outputFile.get().asFile.absolutePath}")
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
        const val SCAN_ENABLED_PROPERTY = "buildIntegration.dependencies.scan.enabled"
        const val COMPONENT_REGISTRY_URL_PROPERTY = "buildIntegration.dependencies.scan.componentsRegistryUrl"
        const val PROJECTS_PROPERTY = "buildIntegration.dependencies.scan.projects"
        const val CONFIGURATIONS_PROPERTY = "buildIntegration.dependencies.scan.configurations"
        const val OUTPUT_FILE_PROPERTY = "buildIntegration.dependencies.outputFile"
    }
}
