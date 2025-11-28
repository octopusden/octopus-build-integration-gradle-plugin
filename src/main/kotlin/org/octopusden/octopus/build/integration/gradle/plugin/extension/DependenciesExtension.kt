package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.octopusden.octopus.build.integration.gradle.plugin.model.Component
import org.octopusden.octopus.build.integration.gradle.plugin.model.ExportDependenciesConfig
import org.octopusden.octopus.build.integration.gradle.plugin.model.ScanConfig

class DependenciesExtension {

    private var teamCityParameter = "DEPENDENCIES"

    private val components = mutableListOf<Component>()
    private val scanExtension = ScanExtension()

    fun add(name: String, version: String) {
        components += Component(name, version)
    }

    fun setTeamCityParameter(value: String) {
        teamCityParameter = value
    }

    fun scan(block: ScanExtension.() -> Unit) {
        scanExtension.setEnabled(true)
        scanExtension.block()
    }

    fun build(): ExportDependenciesConfig =
        ExportDependenciesConfig(
            components = components,
            scan = ScanConfig(
                enabled = scanExtension.isEnabled(),
                componentsRegistryUrl = scanExtension.getComponentsRegistryUrl(),
                projects = scanExtension.getProjects().toRegex(),
                configurations = scanExtension.getConfigurations().toRegex()
            ),
            teamCityParameter = teamCityParameter
        )

}