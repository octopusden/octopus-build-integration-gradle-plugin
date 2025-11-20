package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.octopusden.octopus.build.integration.gradle.plugin.model.ExportDependenciesConfig

open class BuildIntegrationExtension {

    val exportDependenciesBuilder = ExportDependenciesBuilder()

    fun exportDependencies(block: ExportDependenciesBuilder.() -> Unit) = exportDependenciesBuilder.block()

    fun buildConfig(): ExportDependenciesConfig = exportDependenciesBuilder.build()

}