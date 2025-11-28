package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.octopusden.octopus.build.integration.gradle.plugin.model.Component
import org.octopusden.octopus.build.integration.gradle.plugin.model.ExportDependenciesConfig
import org.octopusden.octopus.build.integration.gradle.plugin.model.ScanConfig

class DependenciesExtension {

    private var outputFile: String? = null

    private val components = mutableListOf<Component>()
    private val scanExtension = ScanExtension()

    fun add(name: String, version: String) {
        components += Component(name, version)
    }

    fun setOutputFile(value: String) {
        outputFile = value
    }

    fun scan(block: ScanExtension.() -> Unit) {
        scanExtension.setEnabled(true)
        scanExtension.block()
    }

    fun build(): ExportDependenciesConfig =
        ExportDependenciesConfig(
            components = components,
            scan = ScanConfig(
                enabled = scanExtension.isEnabled() ?: DEFAULT_ENABLED,
                componentsRegistryUrl = scanExtension.getComponentsRegistryUrl() ?: "",
                projects = scanExtension.getProjects()?.toRegex() ?: DEFAULT_PROJECTS.toRegex(),
                configurations = scanExtension.getConfigurations()?.toRegex() ?: DEFAULT_CONFIGURATIONS.toRegex()
            ),
            outputFile = outputFile ?: DEFAULT_OUTPUT_FILE
        )

    companion object {
        const val DEFAULT_OUTPUT_FILE = "export-dependencies-report.txt"
        const val DEFAULT_ENABLED = false
        const val DEFAULT_PROJECTS = ".+"
        const val DEFAULT_CONFIGURATIONS = "runtime.+"
    }
}