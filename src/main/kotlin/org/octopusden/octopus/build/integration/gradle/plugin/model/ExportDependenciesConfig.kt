package org.octopusden.octopus.build.integration.gradle.plugin.model

data class ExportDependenciesConfig (
    val components: List<ComponentSelector>,
    val gradleDependenciesSelector: GradleDependenciesSelector,
    val gradleDependencies: Boolean
)