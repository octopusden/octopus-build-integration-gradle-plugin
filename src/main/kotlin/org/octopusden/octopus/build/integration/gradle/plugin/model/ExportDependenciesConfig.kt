package org.octopusden.octopus.build.integration.gradle.plugin.model

data class ExportDependenciesConfig (
    val components: List<ComponentSelector>,
    val gradleDependenciesSelector: GradleDependenciesSelector,
    val autoRegistration: Boolean,
    val gradleDependencies: Boolean
) {
    val isConfigured: Boolean
        get() = autoRegistration
                || gradleDependencies
                || components.isNotEmpty()
                || gradleDependenciesSelector.excludeModules.isNotEmpty()
                || gradleDependenciesSelector.excludeComponents.isNotEmpty()
                || gradleDependenciesSelector.includeModules.isNotEmpty()
                || gradleDependenciesSelector.includeAllDependencies
}