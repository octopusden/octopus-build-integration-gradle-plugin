package org.octopusden.octopus.build.integration.gradle.plugin.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.octopusden.octopus.build.integration.gradle.plugin.extension.DependenciesExtension.Component

abstract class ExportDependenciesTask : DefaultTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val dependencies: ListProperty<Component>

    @TaskAction
    fun exportDependencies() {
        val jsonResult = ObjectMapper().apply { enable(SerializationFeature.INDENT_OUTPUT) }.writeValueAsString(dependencies.get())
        logger.info("ExportDependenciesTask: resulting dependencies: {}", dependencies.get())
        outputFile.get().asFile.parentFile.mkdirs()
        outputFile.get().asFile.writeText(jsonResult)
        logger.info("ExportDependenciesTask: exported dependencies written to: ${outputFile.get().asFile.absolutePath}")
    }

    companion object {
        const val SCAN_ENABLED_PROPERTY = "buildIntegration.dependencies.scan.enabled"
        const val COMPONENT_REGISTRY_URL_PROPERTY = "buildIntegration.dependencies.scan.componentsRegistryUrl"
        const val PROJECTS_PROPERTY = "buildIntegration.dependencies.scan.projects"
        const val CONFIGURATIONS_PROPERTY = "buildIntegration.dependencies.scan.configurations"
        const val OUTPUT_FILE_PROPERTY = "buildIntegration.dependencies.outputFile"
    }
}
