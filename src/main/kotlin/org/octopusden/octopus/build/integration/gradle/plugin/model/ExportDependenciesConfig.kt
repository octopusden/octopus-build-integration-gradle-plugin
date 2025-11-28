package org.octopusden.octopus.build.integration.gradle.plugin.model

data class ExportDependenciesConfig (
    val components: List<Component>,
    val scan: ScanConfig,
    val outputFile: String
)