package org.octopusden.octopus.build.integration.gradle.plugin.model

data class ExportDependenciesConfig (
    val components: Set<Component>,
    val scan: ScanConfig,
    val outputFile: String
)