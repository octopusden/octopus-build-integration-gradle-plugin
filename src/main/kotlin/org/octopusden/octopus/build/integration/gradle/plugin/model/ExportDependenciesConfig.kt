package org.octopusden.octopus.build.integration.gradle.plugin.model

data class ExportDependenciesConfig (
    val components: List<ComponentSelector>,
    val gradleDependencies: GradleDependenciesSelector,
    val gradleDependenciesEnabled: Boolean
)