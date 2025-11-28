package org.octopusden.octopus.build.integration.gradle.plugin.model

data class ScanConfig (
    val enabled: Boolean,
    val componentsRegistryUrl: String,
    val projects: Regex,
    val configurations: Regex
)