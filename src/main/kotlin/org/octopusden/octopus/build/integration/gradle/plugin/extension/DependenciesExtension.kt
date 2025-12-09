package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.gradle.api.Action
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import java.io.Serializable
import javax.inject.Inject

abstract class DependenciesExtension @Inject constructor(
    objects: ObjectFactory,
    layout: ProjectLayout
) {

    val outputFile: RegularFileProperty = objects.fileProperty()
        .convention(layout.buildDirectory.file(DEFAULT_OUTPUT_FILE))

    val components: SetProperty<Component> = objects.setProperty(Component::class.java)

    val scan: ScanExtension = objects.newInstance(ScanExtension::class.java)

    fun scan(action: Action<in ScanExtension>) {
        action.execute(scan)
    }

    fun scan(block: ScanExtension.() -> Unit) {
        scan(Action { it.block() })
    }

    data class Component (
        val name: String,
        val version: String
    ) : Serializable

    companion object {
        const val DEFAULT_OUTPUT_FILE = "export-dependencies-report.json"
    }
}