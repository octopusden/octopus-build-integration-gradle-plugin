package org.octopusden.octopus.build.integration.gradle.plugin.service.impl

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.octopusden.octopus.build.integration.gradle.plugin.model.Component
import org.octopusden.octopus.build.integration.gradle.plugin.model.ExportDependenciesConfig
import org.octopusden.octopus.build.integration.gradle.plugin.service.ExportDependenciesService
import org.octopusden.octopus.components.registry.client.ComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.core.dto.ArtifactDependency
import org.slf4j.LoggerFactory

class ExportDependenciesServiceImpl (
    private val componentsRegistryClient: ComponentsRegistryServiceClient?
) : ExportDependenciesService {

    override fun getDependencies(project: Project, config: ExportDependenciesConfig): List<String> {
        validateManualComponents(config.components)
        val manualComponents = config.components.map { "${it.name}:${it.version}" }
        val gradleComponents = if (config.scan.enabled) {
            val artifacts = getDependenciesFromGradle(project, config)
                .map { ArtifactDependency(it.group, it.module, it.version) }.toSet()
            mapArtifactsToComponents(artifacts)
        } else {
            emptyList()
        }
        return (manualComponents + gradleComponents).distinct().sorted()
    }

    private fun validateManualComponents(components: List<Component>) {
        val invalidComponents = components
            .filter { !it.version.matches(versionRegex) }
            .map { "DependenciesExportService: Version format not valid ${it.name}:${it.version}" }
        if (invalidComponents.isNotEmpty()) {
            val message = invalidComponents.joinToString("\n")
            logger.error(message)
            throw GradleException(message)
        }
    }

    private fun getDependenciesFromGradle(project: Project, config: ExportDependenciesConfig): Set<ModuleComponentIdentifier> {
        val configurationsByProject: List<Pair<Project, Configuration>> =
            project.rootProject.allprojects.flatMap { subProject ->
                subProject.configurations
                    .filter { it.name.matches(config.scan.configurations) }
                    .map { subProject to it }
            }
        logger.info(
            "DependenciesExportService: Using configurations {}",
            configurationsByProject.map { (project, config) -> "${project.path}:${config.name}" }
        )
        return configurationsByProject.flatMap { (subProject, gradleConfig) ->
            extractDependenciesFromConfiguration(subProject, config, gradleConfig)
        }.toSet()
    }

    private fun extractDependenciesFromConfiguration(project: Project, config: ExportDependenciesConfig, gradleConfig: Configuration): List<ModuleComponentIdentifier> {
        requireNotNull(componentsRegistryClient) {
            "ComponentsRegistryServiceClient must be provided when scan is enabled"
        }
        logger.info("DependenciesExportService: Extract dependencies for configuration '{}'", gradleConfig.name)
        gradleConfig.allDependencies.forEach {
            if (it.version == null) {
                logger.warn(
                    "DependenciesExportService: Dependency {}:{} has no version declared, this may lead to conflicts or unexpected resolution behaviour",
                    it.group, it.name
                )
            }
        }
        val resolvableName = "${gradleConfig.name}Resolvable"
        val resolvableConfig = project.configurations.findByName(resolvableName)
            ?: project.configurations.create(resolvableName).apply {
                isCanBeResolved = true
                isCanBeConsumed = false
                isTransitive = false
                extendsFrom(gradleConfig)
                logger.info(
                    "DependenciesExportService: Created resolvable configuration '{}:{}' extending '{}'",
                    project.path, resolvableName, gradleConfig.name
                )
            }
        val allResolvedIds = resolvableConfig.incoming.resolutionResult.allDependencies.mapNotNull {
            if (it is ResolvedDependencyResult && it.selected.id is ModuleComponentIdentifier) {
                it.selected.id as ModuleComponentIdentifier
            } else null
        }
        val supportedGroupIds = componentsRegistryClient.getSupportedGroupIds()
        return allResolvedIds
            .filter { matchesSupportedGroup(it, supportedGroupIds) }
            .filter { matchesProjects(it, config.scan.projects) }
    }

    private fun matchesSupportedGroup(id: ModuleComponentIdentifier, supportedGroupIds: Set<String>): Boolean {
        val passed = supportedGroupIds.any { prefix -> id.group.startsWith(prefix) }
        logger.info("DependenciesExportService: SupportedGroupIds filter {} passed={}", id, passed)
        return passed
    }

    private fun matchesProjects(id: ModuleComponentIdentifier, projects: Regex): Boolean {
        val passed = id.module.matches(projects)
        logger.info("DependenciesExportService: Projects filter {} passed={}", id, passed)
        return passed
    }

    private fun mapArtifactsToComponents(artifacts: Set<ArtifactDependency>): List<String> {
        requireNotNull(componentsRegistryClient) {
            "ComponentsRegistryServiceClient must be provided when scan is enabled"
        }
        if (artifacts.isEmpty()) return emptyList()
        val response = componentsRegistryClient.findArtifactComponentsByArtifacts(artifacts)
        return response.artifactComponents.mapNotNull { artifactComponent ->
            val comp = artifactComponent.component
            if (comp == null) {
                logger.error("DependenciesExportService: Component not found by artifact {}", artifactComponent.artifact)
                null
            } else {
                "${comp.id}:${comp.version}"
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExportDependenciesServiceImpl::class.java)
        private val versionRegex = Regex("\\d+([._-]\\d+)*")
    }

}