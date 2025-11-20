package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.octopusden.octopus.build.integration.gradle.plugin.model.ComponentSelector
import org.octopusden.octopus.build.integration.gradle.plugin.model.ModuleSelector


class GradleDependenciesBuilder {

    var includeAllDependencies = false

    internal val includeModules = mutableListOf<ModuleSelector>()
    internal val excludeModules = mutableListOf<ModuleSelector>()
    internal val excludeComponents = mutableListOf<ComponentSelector>()

    fun includeModule(group: String? = null, module: String? = null) {
        includeModules += ModuleSelector(group, module)
    }

    fun excludeModule(group: String? = null, module: String? = null) {
        excludeModules += ModuleSelector(group, module)
    }

    fun excludeComponent(id: String) {
        excludeComponents += ComponentSelector(id, null)
    }

}