package org.octopusden.octopus.build.integration.gradle.plugin.service

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.octopusden.octopus.build.integration.gradle.plugin.extension.DependenciesExtension.Component
import org.octopusden.octopus.components.registry.client.ComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.core.dto.ArtifactDependency
import org.slf4j.LoggerFactory

class DependenciesExtractor(
    private val componentsRegistryClient: ComponentsRegistryServiceClient?
) {

    fun getDependencies(
        project: Project,
        manualComponents: Set<Component>,
        scanEnabled: Boolean,
        projects: Regex,
        configurations: Regex
    ): List<Component> {
        validateManualComponents(manualComponents)
        val gradleComponents = if (scanEnabled) {
            val artifacts = getDependenciesFromGradle(project, projects, configurations)
                .map { ArtifactDependency(it.group, it.module, it.version) }
                .toSet()
            mapArtifactsToComponents(artifacts)
        } else {
            emptyList()
        }
        return (manualComponents + gradleComponents)
            .distinct()
            .sortedWith(compareBy<Component> { it.name }.thenBy { it.version })
    }

    private fun validateManualComponents(components: Set<Component>) {
        val invalidComponents = components
            .filter { !it.version.matches(versionRegex) }
            .map { "DependenciesExtractor: Version format not valid ${it.name}:${it.version}" }
        if (invalidComponents.isNotEmpty()) {
            val message = invalidComponents.joinToString("\n")
            logger.error(message)
            throw GradleException(message)
        }
    }

    private fun getDependenciesFromGradle(
        project: Project,
        projects: Regex,
        configurations: Regex
    ): Set<ModuleComponentIdentifier> {
        val configurationsByProject: List<Pair<Project, Configuration>> =
            project.rootProject.allprojects.flatMap { subProject ->
                subProject.configurations
                    .filter { it.name.matches(configurations) }
                    .map { subProject to it }
            }
        logger.info(
            "DependenciesExtractor: Using configurations {}",
            configurationsByProject.map { (project, config) -> "${project.path}:${config.name}" }
        )
        return configurationsByProject.flatMap { (subProject, gradleConfig) ->
            extractDependenciesFromConfiguration(subProject, projects, gradleConfig)
        }.toSet()
    }

    private fun extractDependenciesFromConfiguration(
        project: Project,
        projects: Regex,
        gradleConfig: Configuration
    ): List<ModuleComponentIdentifier> {
        requireNotNull(componentsRegistryClient) { "ComponentsRegistryServiceClient must be provided when scan is enabled" }
        logger.info("DependenciesExtractor: Extract dependencies for configuration '{}'", gradleConfig.name)
        gradleConfig.allDependencies.forEach {
            if (it.version == null) {
                logger.warn(
                    "DependenciesExtractor: Dependency {}:{} has no version declared, this may lead to conflicts or unexpected resolution behaviour",
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
                    "DependenciesExtractor: Created resolvable configuration '{}:{}' extending '{}'",
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
            .filter { matchesProjects(it, projects) }
    }

    private fun matchesSupportedGroup(id: ModuleComponentIdentifier, supportedGroupIds: Set<String>): Boolean {
        val passed = supportedGroupIds.any { prefix -> id.group.startsWith(prefix) }
        logger.info("DependenciesExtractor: SupportedGroupIds filter {} passed={}", id, passed)
        return passed
    }

    private fun matchesProjects(id: ModuleComponentIdentifier, projects: Regex): Boolean {
        val passed = id.module.matches(projects)
        logger.info("DependenciesExtractor: Projects filter {} passed={}", id, passed)
        return passed
    }

    private fun mapArtifactsToComponents(artifacts: Set<ArtifactDependency>): List<Component> {
        requireNotNull(componentsRegistryClient) { "ComponentsRegistryServiceClient must be provided when scan is enabled" }
        if (artifacts.isEmpty()) return emptyList()
        val response = componentsRegistryClient.findArtifactComponentsByArtifacts(artifacts)
        return response.artifactComponents.mapNotNull {
            val comp = it.component
            if (comp == null) {
                logger.error("DependenciesExtractor: Component not found by artifact {}", it.artifact)
                null
            } else {
                Component(comp.id, comp.version)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DependenciesExtractor::class.java)
        private val versionRegex = Regex("\\d+([._-]\\d+)*")
    }

}