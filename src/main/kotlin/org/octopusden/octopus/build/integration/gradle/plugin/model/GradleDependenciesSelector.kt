package org.octopusden.octopus.build.integration.gradle.plugin.model

data class GradleDependenciesSelector (
    val includeModules: List<ModuleSelector> = emptyList(),
    val excludeModules: List<ModuleSelector> = emptyList(),
    val excludeComponents: List<ComponentSelector> = emptyList(),
    val includeAllDependencies: Boolean
)
