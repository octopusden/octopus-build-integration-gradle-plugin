package org.octopusden.octopus.build.integration.gradle.plugin.service

import org.gradle.api.Project
import org.octopusden.octopus.build.integration.gradle.plugin.model.Component
import org.octopusden.octopus.build.integration.gradle.plugin.model.ExportDependenciesConfig

interface ExportDependenciesService {
    fun getDependencies(project: Project, config: ExportDependenciesConfig) : List<Component>
}