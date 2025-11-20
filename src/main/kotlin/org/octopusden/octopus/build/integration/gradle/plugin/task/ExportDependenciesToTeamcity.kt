package org.octopusden.octopus.build.integration.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.octopusden.octopus.build.integration.gradle.plugin.extension.BuildIntegrationExtension
import org.octopusden.octopus.build.integration.gradle.plugin.service.DependenciesExportService
import org.octopusden.octopus.build.integration.gradle.plugin.service.impl.DependenciesExportServiceImpl
import org.octopusden.octopus.components.registry.client.ComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClientUrlProvider

abstract class ExportDependenciesToTeamcity : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val excludedConfigurations: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val includedConfigurations: ListProperty<String>

    private val componentsRegistryUrl = project.findProperty(COMPONENT_REGISTRY_SERVICE_URL_PROPERTY)?.toString()
        ?: throw GradleException("$COMPONENT_REGISTRY_SERVICE_URL_PROPERTY must not be empty")

    init {
        excludedConfigurations.convention(emptyList())
        includedConfigurations.convention(listOf("runtimeElements", "runtimeClasspath"))
    }

    @TaskAction
    fun exportDependencies() {
        val extension = project.extensions.findByType(BuildIntegrationExtension::class.java)
            ?: throw GradleException("BuildIntegrationExtension is not registered!")
        val exportConfig = extension.buildConfig()
        val includeAllDependencies = project.booleanPropertyOrDefault(
            exportConfig.gradleDependenciesSelector.includeAllDependencies,
            INCLUDE_ALL_DEPENDENCIES_PROPERTY
        )
        logger.info(
            "ExportDependenciesToTeamcity started. excludedConfigurations={}, includedConfigurations={}, includeAllDependencies={}, componentsRegistryUrl={}",
            excludedConfigurations.get(),
            includedConfigurations.get(),
            includeAllDependencies,
            componentsRegistryUrl
        )
        val componentsRegistryClient = createComponentsRegistryClient()
        val exportService: DependenciesExportService = DependenciesExportServiceImpl(componentsRegistryClient)
        val dependencies = exportService.getDependencies(
            project = project,
            config = exportConfig,
            includedConfigurations = includedConfigurations.get(),
            excludedConfigurations = excludedConfigurations.get(),
            includeAllDependencies = includeAllDependencies
        )
        if (dependencies.isEmpty()) {
            logger.info("ExportDependenciesToTeamcity: no dependencies to export, DEPENDENCIES parameter will not be set")
            return
        }
        val value = dependencies.joinToString(",")
        logger.info("ExportDependenciesToTeamcity: resulting dependencies: {}", value)
        val firstSupported = componentsRegistryClient.getSupportedGroupIds().firstOrNull().orEmpty()
        val effectiveConfigs = includedConfigurations.get().filterNot { excludedConfigurations.get().contains(it) }
        logger.info(
            "ExportDependenciesToTeamcity: only {}.* dependencies from {} will be registered by release management",
            firstSupported,
            effectiveConfigs
        )
        val escaped = escapeTeamCityValue(value)
        println("##teamcity[setParameter name='DEPENDENCIES' value='$escaped']")
    }

    private fun escapeTeamCityValue(value: String): String =
        value.replace("|", "||")
            .replace("'", "|'")
            .replace("[", "|[")
            .replace("]", "|]")
            .replace("\n", "|n")
            .replace("\r", "|r")

    private fun createComponentsRegistryClient(): ComponentsRegistryServiceClient =
        ClassicComponentsRegistryServiceClient(
            object : ClassicComponentsRegistryServiceClientUrlProvider {
                override fun getApiUrl(): String = componentsRegistryUrl
            }
        )

    companion object {
        private const val COMPONENT_REGISTRY_SERVICE_URL_PROPERTY = "components-registry-service-url"
        const val INCLUDE_ALL_DEPENDENCIES_PROPERTY = "include-all-dependencies"

        fun Project.booleanPropertyOrDefault(default: Boolean, propertyName: String): Boolean {
            val raw = findProperty(propertyName)?.toString()
            return raw?.toBoolean() ?: default
        }
    }
}
