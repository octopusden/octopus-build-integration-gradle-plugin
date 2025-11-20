package org.octopusden.octopus.build.integration.gradle.plugin.service.impl

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.octopusden.octopus.build.integration.gradle.plugin.model.ComponentSelector
import org.octopusden.octopus.build.integration.gradle.plugin.model.ExportDependenciesConfig
import org.octopusden.octopus.build.integration.gradle.plugin.model.GradleDependenciesSelector
import org.octopusden.octopus.build.integration.gradle.plugin.model.ModuleSelector
import org.octopusden.octopus.build.integration.gradle.plugin.service.DependenciesExportService
import org.octopusden.octopus.components.registry.client.ComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.core.dto.ArtifactDependency
import org.slf4j.LoggerFactory

class DependenciesExportServiceImpl (
    private val componentsRegistryClient: ComponentsRegistryServiceClient
) : DependenciesExportService {

    override fun getDependencies(
        project: Project,
        config: ExportDependenciesConfig,
        includedConfigurations: List<String>,
        excludedConfigurations: List<String>,
        includeAllDependencies: Boolean
    ): List<String> {
        validateManualComponents(config.components)
        val manualComponents = config.components.map { "${it.id}:${it.version}" }
        val fromGradleComponents = if (config.gradleDependencies || includeAllDependencies) {
            val artifactDeps = collectArtifactDependenciesFromGradle(
                project = project,
                config = config,
                includedConfigurations = includedConfigurations,
                excludedConfigurations = excludedConfigurations,
                includeAllDependencies = includeAllDependencies
            )
            mapArtifactsToComponents(artifactDeps)
        } else {
            emptyList()
        }
        return (manualComponents + fromGradleComponents).distinct().sorted()
    }

    private fun validateManualComponents(components: List<ComponentSelector>) {
        val invalidComponents = components
            .filter { it.version == null || !it.version.matches(versionRegex) }
            .map { "DependenciesExportService: Version format not valid ${it.id}:${it.version}" }
        if (invalidComponents.isNotEmpty()) {
            val message = invalidComponents.joinToString("\n")
            logger.error(message)
            throw GradleException(message)
        }
    }

    private fun collectArtifactDependenciesFromGradle(
        project: Project,
        config: ExportDependenciesConfig,
        includedConfigurations: List<String>,
        excludedConfigurations: List<String>,
        includeAllDependencies: Boolean
    ): Set<ArtifactDependency> {
        val selector = config.gradleDependenciesSelector
        val configurationsByProject: List<Pair<Project, Configuration>> =
            project.rootProject.allprojects.flatMap { subProject ->
                subProject.configurations
                    .filter { config ->
                        !excludedConfigurations.contains(config.name) &&
                                (includedConfigurations.isEmpty() || includedConfigurations.contains(config.name))
                    }
                    .map { cfg -> subProject to cfg }
            }
        logger.info(
            "DependenciesExportService: using configurations {} (excluded={})",
            configurationsByProject.map { (project, config) -> "${project.path}:${config.name}" },
            excludedConfigurations
        )
        val moduleIds = configurationsByProject.flatMap { (subProject, config) ->
            extractDependenciesFromConfiguration(
                project = subProject,
                configuration = config,
                selector = selector,
                includeAllDependencies = includeAllDependencies
            )
        }
        return moduleIds.map { ArtifactDependency(it.group, it.module, it.version) }.toSet()
    }


    private fun extractDependenciesFromConfiguration(
        project: Project,
        configuration: Configuration,
        selector: GradleDependenciesSelector,
        includeAllDependencies: Boolean
    ): List<ModuleComponentIdentifier> {
        logger.info("DependenciesExportService: extract dependencies for configuration '{}'", configuration.name)
        configuration.allDependencies.forEach { it ->
            if (it.version == null) {
                logger.warn("Dependency {}:{} has no version declared, this may lead to conflicts or unexpected resolution behaviour",
                    it.group, it.name
                )
            }
        }
        val resolvableName = configuration.name + "Resolvable"
        val resolvableConfig = project.configurations.findByName(resolvableName)
            ?: project.configurations.create(resolvableName).apply {
                isCanBeResolved = true
                isCanBeConsumed = false
                isTransitive = false
                extendsFrom(configuration)
                logger.info(
                    "DependenciesExportService: created resolvable configuration '{}:{}' extending '{}'",
                    project.path, resolvableName, configuration.name
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
            .filter { matchesExcludeFilter(it, selector) }
            .filter { matchesIncludeFilter(it, selector, includeAllDependencies) }
    }

    private fun matchesSupportedGroup(id: ModuleComponentIdentifier, supportedGroupIds: Set<String>): Boolean {
        val passed = supportedGroupIds.any { prefix -> id.group.startsWith(prefix) }
        logger.info("DependenciesExportService: supportedGroupIds filter {} passed={}", id, passed)
        return passed
    }

    private fun matchesExcludeFilter(id: ModuleComponentIdentifier, selector: GradleDependenciesSelector): Boolean {
        val passed = selector.excludeModules.none { moduleMatchesSelector(id, it) }
        logger.info("DependenciesExportService: exclude filter {} passed={}", id, passed)
        return passed
    }

    private fun matchesIncludeFilter(
        id: ModuleComponentIdentifier,
        selector: GradleDependenciesSelector,
        includeAllDependencies: Boolean
    ): Boolean {
        if (includeAllDependencies) {
            logger.info("DependenciesExportService: include filter {} passed={} (includeAllDependencies=true)", id, true)
            return true
        }
        val passed = selector.includeModules.any { moduleMatchesSelector(id, it) }
        logger.info("DependenciesExportService: include filter {} passed={}", id, passed)
        return passed
    }

    private fun moduleMatchesSelector(id: ModuleComponentIdentifier, selector: ModuleSelector): Boolean {
        val groupMatches = selector.group?.let { it == id.group } ?: true
        val nameMatches = selector.name?.let { it == id.module } ?: true
        return groupMatches && nameMatches
    }

    private fun mapArtifactsToComponents(artifacts: Set<ArtifactDependency>): List<String> {
        if (artifacts.isEmpty()) return emptyList()
        val response = componentsRegistryClient.findArtifactComponentsByArtifacts(artifacts)
        return response.artifactComponents.mapNotNull { artifactComponent ->
            val comp = artifactComponent.component
            if (comp == null) {
                logger.error("DependenciesExportService: component not found by artifact {}", artifactComponent.artifact)
                null
            } else {
                "${comp.id}:${comp.version}"
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DependenciesExportServiceImpl::class.java)
        private val versionRegex = Regex("\\d+([._-]\\d+)*")
    }

}