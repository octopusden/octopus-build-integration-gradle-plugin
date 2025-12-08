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
import java.util.regex.PatternSyntaxException

class DependenciesExtractor(
    private val project: Project,
    private val manualComponents: Set<Component>,
    private val scanEnabled: Boolean,
    private val componentsRegistryUrl: String,
    private val projects: String,
    private val configurations: String
) {

    private val componentsRegistryClient: ComponentsRegistryServiceClient by lazy {
        createComponentsRegistryClient(componentsRegistryUrl)
    }

    fun extract(): List<Component> {
        validateManualComponents(manualComponents)
        val gradleComponents = if (scanEnabled) {
            val artifacts = getDependenciesFromGradle()
                .map { ArtifactDependency(it.group, it.module, it.version) }
                .toSet()
            mapArtifactsToComponents(artifacts)
        } else {
            emptyList()
        }
        return (manualComponents + gradleComponents).distinct()
            .sortedWith(compareBy<Component> { it.name }.thenBy { it.version })
    }

    private fun validateManualComponents(components: Set<Component>) {
        val invalidComponents = components.filter { !it.version.matches(versionRegex) }
            .map { "DependenciesExtractor: Version format not valid ${it.name}:${it.version}" }
        if (invalidComponents.isNotEmpty()) {
            val message = invalidComponents.joinToString("\n")
            logger.error(message)
            throw GradleException(message)
        }
    }

    private fun getDependenciesFromGradle(): Set<ModuleComponentIdentifier> {
        val configurationsRegex: Regex = regexProcessing(configurations)
        val configurationsByProject: List<Pair<Project, Configuration>> =
            project.rootProject.allprojects.flatMap { subProject ->
                subProject.configurations
                    .filter { it.name.matches(configurationsRegex) }
                    .map { subProject to it }
            }
        logger.info(
            "DependenciesExtractor: Using configurations {}",
            configurationsByProject.map { (project, config) -> "${project.path}:${config.name}" }
        )
        return configurationsByProject.flatMap { (subProject, gradleConfig) ->
            extractDependenciesFromConfiguration(subProject, gradleConfig)
        }.toSet()
    }

    private fun extractDependenciesFromConfiguration(
        project: Project,
        gradleConfig: Configuration
    ): List<ModuleComponentIdentifier> {
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
            .filter { matchesProjects(it) }
    }

    private fun matchesSupportedGroup(id: ModuleComponentIdentifier, supportedGroupIds: Set<String>): Boolean {
        val passed = supportedGroupIds.any { prefix -> id.group.startsWith(prefix) }
        logger.info("DependenciesExtractor: SupportedGroupIds filter {} passed={}", id, passed)
        return passed
    }

    private fun matchesProjects(id: ModuleComponentIdentifier): Boolean {
        val projectsRegex: Regex = regexProcessing(projects)
        val passed = id.module.matches(projectsRegex)
        logger.info("DependenciesExtractor: Projects filter {} passed={}", id, passed)
        return passed
    }

    private fun mapArtifactsToComponents(artifacts: Set<ArtifactDependency>): List<Component> {
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
        private val logger = LoggerFactory.getLogger(DependenciesExtractor::class.java)
        private val versionRegex = Regex("\\d+([._-]\\d+)*")
    }

}