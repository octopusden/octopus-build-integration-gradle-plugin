package org.octopusden.octopus.build.integration.gradle.plugin.service

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.octopusden.octopus.build.integration.gradle.plugin.extension.DependenciesExtension.Component
import org.octopusden.octopus.components.registry.client.ComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClientUrlProvider
import org.octopusden.octopus.components.registry.core.dto.ArtifactDependency
import org.slf4j.LoggerFactory

class DependenciesExtractor(
    private val project: Project,
    private val componentsRegistryUrl: String,
    projectsPattern: String,
    configurationsPattern: String
) {

    private val componentsRegistryClient: ComponentsRegistryServiceClient =
        ClassicComponentsRegistryServiceClient(
            object : ClassicComponentsRegistryServiceClientUrlProvider {
                override fun getApiUrl(): String = componentsRegistryUrl
            }
        )

    private val projectsRegex = projectsPattern.toRegex()

    private val configurationsRegex = configurationsPattern.toRegex()

    fun extract(): Set<Component> {
        val artifacts = getDependenciesFromGradle().map { ArtifactDependency(it.group, it.module, it.version) }.toSet()
        return if (artifacts.isEmpty()) emptySet()
        else try {
            artifacts.chunked(50).flatMap { chunk ->
                componentsRegistryClient.findArtifactComponentsByArtifacts(chunk.toSet()).artifactComponents.mapNotNull {
                    if (it.component == null)
                        logger.warn("DependenciesExtractor: Component not found by artifact {}", it.artifact)
                    it.component?.let { component ->
                        Component(component.id, component.version)
                    }
                }
            }.toSet()
        } catch (e: Exception) {
            throw GradleException("Failed to query Components Registry at '$componentsRegistryUrl': ${e.message}", e)
        }
    }

    private fun getDependenciesFromGradle(): Set<ModuleComponentIdentifier> {
        val configurationsByProject: List<Pair<Project, Configuration>> =
            project.rootProject.allprojects
                .filter { matchesProjects(it) }
                .flatMap { subProject ->
                    subProject.configurations.filter { matchesConfigurations(it) }.map { subProject to it }
                }
        logger.info(
            "DependenciesExtractor: Using configurations {}",
            configurationsByProject.map { (project, config) -> "${project.path}:${config.name}" }
        )
        return configurationsByProject.flatMap { (subProject, gradleConfig) ->
            extractDependenciesFromConfiguration(subProject, gradleConfig)
        }.toSet()
    }

    private fun extractDependenciesFromConfiguration(project: Project, gradleConfig: Configuration): Set<ModuleComponentIdentifier> {
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
                logger.debug(
                    "DependenciesExtractor: Created resolvable configuration '{}:{}' extending '{}'",
                    project.path, resolvableName, gradleConfig.name
                )
            }
        val allResolvedIds = resolvableConfig.incoming.resolutionResult.allDependencies.mapNotNull {
            if (it is ResolvedDependencyResult && it.selected.id is ModuleComponentIdentifier) {
                it.selected.id as ModuleComponentIdentifier
            } else null
        }
        val supportedGroupIds = try {
            componentsRegistryClient.getSupportedGroupIds()
        } catch (e: Exception) {
            throw GradleException("Failed to query Components Registry at '$componentsRegistryUrl': ${e.message}", e)
        }
        return allResolvedIds.filter { matchesSupportedGroups(it, supportedGroupIds) }.toSet()
    }

    private fun matchesProjects(subproject: Project): Boolean {
        val passed = subproject.path.matches(projectsRegex)
        logger.debug("DependenciesExtractor: Projects filter {} passed={}", subproject.path, passed)
        return passed
    }

    private fun matchesConfigurations(configuration: Configuration): Boolean {
        val passed = configuration.name.matches(configurationsRegex)
        logger.debug("DependenciesExtractor: Configurations filter {} passed={}", configuration.name, passed)
        return passed
    }

    private fun matchesSupportedGroups(id: ModuleComponentIdentifier, supportedGroupIds: Set<String>): Boolean {
        val passed = supportedGroupIds.any { id.group.startsWith(it) }
        logger.debug("DependenciesExtractor: SupportedGroups filter {} passed={}", id, passed)
        return passed
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DependenciesExtractor::class.java)
    }

}