package org.octopusden.octopus.build.integration.gradle.plugin.service

import org.gradle.api.Project
import org.octopusden.octopus.build.integration.gradle.plugin.model.ExportDependenciesConfig

interface DependenciesExportService {
    fun getDependencies(
        project: Project,
        config: ExportDependenciesConfig,
        includedConfigurations: List<String>,
        excludedConfigurations: List<String>,
        includeAllDependencies: Boolean
    ) : List<String>
}