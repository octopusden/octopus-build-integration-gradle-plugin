package org.octopusden.octopus.build.integration.gradle.plugin.task

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

abstract class ExportDependencies : DefaultTask() {

    private val scanEnabled = project.findProperty(SCAN_ENABLED_PROPERTY)?.toString()?.toBoolean()

    private val componentsRegistryUrl = project.findProperty(COMPONENT_REGISTRY_URL_PROPERTY)?.toString()

    private val projects = project.findProperty(PROJECTS_PROPERTY)?.toString()?.toRegex()

    private val configurations = project.findProperty(CONFIGURATIONS_PROPERTY)?.toString()?.toRegex()

    private val outputFile = project.findProperty(OUTPUT_FILE_PROPERTY)?.toString()

    @TaskAction
    fun exportDependencies() {
        val extension = project.extensions.findByType(BuildIntegrationExtension::class.java)
            ?: throw GradleException("BuildIntegrationExtension is not registered!")
        val config = buildExportDependenciesConfig(extension.buildConfig())
        logger.info("ExportDependenciesToTeamcity started. config={}", config)
        val componentsRegistryClient = if (config.scan.enabled) {
            if (config.scan.componentsRegistryUrl.isBlank()) {
                throw GradleException("buildIntegration.dependencies.scan.enabled=true, but componentsRegistryUrl is null or empty")
            }
            val client = createComponentsRegistryClient(config.scan.componentsRegistryUrl)
            logger.info("ExportDependenciesToTeamcity: supported group ids: {}", client.getSupportedGroupIds())
            client
        } else {
            logger.info("ExportDependenciesToTeamcity: scan disabled, components registry client will not be created")
            null
        }
        val exportService: ExportDependenciesService = ExportDependenciesServiceImpl(componentsRegistryClient)
        val dependencies = exportService.getDependencies(project, config)
        if (dependencies.isEmpty()) {
            logger.info("ExportDependenciesToTeamcity: no dependencies to export, ${config.outputFile} parameter will not be set")
            return
        }
        val value = dependencies.joinToString(",")
        logger.info("ExportDependenciesToTeamcity: resulting dependencies: {}", value)
        val outputFile = project.layout.buildDirectory.file(config.outputFile).get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(value)
        logger.info("Exported dependencies written to: ${outputFile.absolutePath}")
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
    }
}
