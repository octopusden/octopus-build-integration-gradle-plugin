package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.octopusden.octopus.build.integration.gradle.plugin.model.ExportDependenciesConfig
import org.octopusden.octopus.build.integration.gradle.plugin.model.GradleDependenciesSelector

class ExportDependenciesBuilder {

    var autoRegistration: Boolean = false
    private val componentsBuilder = ComponentsBuilder()
    private val gradleDependenciesBuilder = GradleDependenciesBuilder()
    private var gradleDependenciesEnabled = false

    fun components(block: ComponentsBuilder.() -> Unit) {
        componentsBuilder.block()
    }

    fun gradleDependencies(block: GradleDependenciesBuilder.() -> Unit) {
        gradleDependenciesEnabled = true
        gradleDependenciesBuilder.block()
    }

    fun build(): ExportDependenciesConfig =
        ExportDependenciesConfig(
            components = componentsBuilder.components.toList(),
            gradleDependenciesSelector = GradleDependenciesSelector(
                includeModules = gradleDependenciesBuilder.includeModules.toList(),
                excludeModules = gradleDependenciesBuilder.excludeModules.toList(),
                excludeComponents = gradleDependenciesBuilder.excludeComponents.toList(),
                includeAllDependencies = gradleDependenciesBuilder.includeAllDependencies
            ),
            autoRegistration = autoRegistration,
            gradleDependencies = gradleDependenciesEnabled
        )

}