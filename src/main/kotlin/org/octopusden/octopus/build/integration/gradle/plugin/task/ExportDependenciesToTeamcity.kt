package org.octopusden.octopus.build.integration.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.octopusden.octopus.build.integration.gradle.plugin.extension.BuildIntegrationExtension
import org.octopusden.octopus.build.integration.gradle.plugin.service.ExportDependenciesService
import org.octopusden.octopus.build.integration.gradle.plugin.service.impl.ExportDependenciesServiceImpl
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

    private val includeAllDependencies = project.findProperty(INCLUDE_ALL_DEPENDENCIES_PROPERTY)?.toString()?.toBoolean()

    init {
        excludedConfigurations.convention(emptyList())
        includedConfigurations.convention(listOf("runtimeElements", "runtimeClasspath"))
    }

    @TaskAction
    fun exportDependencies() {
        val extension = project.extensions.findByType(BuildIntegrationExtension::class.java)
            ?: throw GradleException("BuildIntegrationExtension is not registered!")
        val exportConfig = extension.buildConfig()
        val finalIncludeAllDependencies = includeAllDependencies ?: exportConfig.gradleDependencies.includeAllDependencies
        logger.info(
            "ExportDependenciesToTeamcity started. excludedConfigurations={}, includedConfigurations={}, includeAllDependencies={}, componentsRegistryUrl={}",
            excludedConfigurations.get(),
            includedConfigurations.get(),
            finalIncludeAllDependencies,
            componentsRegistryUrl
        )
        val componentsRegistryClient = createComponentsRegistryClient()
        val exportService: ExportDependenciesService = ExportDependenciesServiceImpl(componentsRegistryClient)
        val dependencies = exportService.getDependencies(
            project = project,
            config = exportConfig,
            includedConfigurations = includedConfigurations.get(),
            excludedConfigurations = excludedConfigurations.get(),
            includeAllDependencies = finalIncludeAllDependencies
        )
        if (dependencies.isEmpty()) {
            logger.info("ExportDependenciesToTeamcity: no dependencies to export, DEPENDENCIES parameter will not be set")
            return
        }
        val value = dependencies.joinToString(",")
        logger.info("ExportDependenciesToTeamcity: resulting dependencies: {}", value)
        val supportedGroupIds = componentsRegistryClient.getSupportedGroupIds()
        val effectiveConfigs = includedConfigurations.get().filterNot { excludedConfigurations.get().contains(it) }
        logger.info(
            "ExportDependenciesToTeamcity: only {}.* dependencies from {} will be registered by release management",
            supportedGroupIds,
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
        const val COMPONENT_REGISTRY_SERVICE_URL_PROPERTY = "components-registry-service.url"
        private const val INCLUDE_ALL_DEPENDENCIES_PROPERTY = "include-all-dependencies"
    }
}
