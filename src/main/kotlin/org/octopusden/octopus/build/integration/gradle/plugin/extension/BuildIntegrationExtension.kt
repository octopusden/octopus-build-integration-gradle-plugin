package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.octopusden.octopus.build.integration.gradle.plugin.model.ExportDependenciesConfig

open class BuildIntegrationExtension {

    val dependenciesExtension = DependenciesExtension()

    fun dependencies(block: DependenciesExtension.() -> Unit) = dependenciesExtension.block()

    fun buildConfig(): ExportDependenciesConfig = dependenciesExtension.build()

}