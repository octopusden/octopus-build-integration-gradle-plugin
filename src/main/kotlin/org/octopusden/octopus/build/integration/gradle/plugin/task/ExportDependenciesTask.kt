package org.octopusden.octopus.build.integration.gradle.plugin.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.octopusden.octopus.build.integration.gradle.plugin.extension.BuildIntegrationExtension
import org.octopusden.octopus.build.integration.gradle.plugin.model.ExportDependenciesConfig
import org.octopusden.octopus.build.integration.gradle.plugin.model.ScanConfig
import org.octopusden.octopus.build.integration.gradle.plugin.service.ExportDependenciesService
import org.octopusden.octopus.build.integration.gradle.plugin.service.impl.ExportDependenciesServiceImpl
import org.octopusden.octopus.components.registry.client.ComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClientUrlProvider
import java.util.regex.PatternSyntaxException

abstract class ExportDependenciesTask : DefaultTask() {

    private val scanEnabled = project.findProperty(SCAN_ENABLED_PROPERTY)?.toString()?.toBoolean()

    private val componentsRegistryUrl = project.findProperty(COMPONENT_REGISTRY_URL_PROPERTY)?.toString()

    private val projects = regexProcessing(project.findProperty(PROJECTS_PROPERTY)?.toString())

    private val configurations = regexProcessing(project.findProperty(CONFIGURATIONS_PROPERTY)?.toString())

    private val outputFile = project.findProperty(OUTPUT_FILE_PROPERTY)?.toString()

    @TaskAction
    fun exportDependencies() {
        val extension = project.extensions.findByType(BuildIntegrationExtension::class.java)
            ?: throw GradleException("BuildIntegrationExtension is not registered!")
        val config = buildExportDependenciesConfig(extension.buildConfig())
        logger.info("ExportDependenciesTask started. config={}", config)
        val componentsRegistryClient = if (config.scan.enabled) {
            if (config.scan.componentsRegistryUrl.isBlank()) {
                throw GradleException("$SCAN_ENABLED_PROPERTY=true, but componentsRegistryUrl is null or empty")
            }
            val client = createComponentsRegistryClient(config.scan.componentsRegistryUrl)
            logger.info("ExportDependenciesTask: supported group ids: {}", client.getSupportedGroupIds())
            client
        } else {
            logger.info("ExportDependenciesTask: scan disabled, components registry client will not be created")
            null
        }
        val exportService: ExportDependenciesService = ExportDependenciesServiceImpl(componentsRegistryClient)
        val dependencies = exportService.getDependencies(project, config)
        val jsonResult = ObjectMapper().apply { enable(SerializationFeature.INDENT_OUTPUT) }.writeValueAsString(dependencies)
        logger.info("ExportDependenciesTask: resulting dependencies: {}", dependencies)
        val outputFile = project.layout.buildDirectory.file(config.outputFile).get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(jsonResult)
        logger.info("ExportDependenciesTask: exported dependencies written to: ${outputFile.absolutePath}")
    }

    private fun buildExportDependenciesConfig(config: ExportDependenciesConfig): ExportDependenciesConfig {
        return ExportDependenciesConfig(
            components = config.components,
            scan = ScanConfig(
                enabled = scanEnabled ?: config.scan.enabled,
                componentsRegistryUrl = componentsRegistryUrl ?: config.scan.componentsRegistryUrl,
                projects = projects ?: config.scan.projects,
                configurations = configurations ?: config.scan.configurations
            ),
            outputFile = outputFile ?: config.outputFile
        )
    }

    private fun createComponentsRegistryClient(componentsRegistryUrl: String): ComponentsRegistryServiceClient =
        ClassicComponentsRegistryServiceClient(
            object : ClassicComponentsRegistryServiceClientUrlProvider {
                override fun getApiUrl(): String = componentsRegistryUrl
            }
        )

    companion object {
        const val SCAN_ENABLED_PROPERTY = "buildIntegration.dependencies.scan.enabled"
        const val COMPONENT_REGISTRY_URL_PROPERTY = "buildIntegration.dependencies.scan.componentsRegistryUrl"
        const val PROJECTS_PROPERTY = "buildIntegration.dependencies.scan.projects"
        const val CONFIGURATIONS_PROPERTY = "buildIntegration.dependencies.scan.configurations"
        const val OUTPUT_FILE_PROPERTY = "buildIntegration.dependencies.outputFile"

        fun regexProcessing(pattern: String?): Regex? {
            if (pattern == null) return null
            return try {
                pattern.toRegex()
            } catch (e: PatternSyntaxException) {
                throw GradleException("Invalid regex pattern: $pattern", e)
            }
        }
    }
}
