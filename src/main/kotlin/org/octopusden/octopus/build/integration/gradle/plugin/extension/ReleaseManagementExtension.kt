package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.gradle.api.Action
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class ReleaseManagementExtension
    @Inject
    constructor(
        objects: ObjectFactory,
        layout: ProjectLayout,
    ) {
        val outputFile: RegularFileProperty = objects
            .fileProperty()
            .convention(layout.buildDirectory.file(DEFAULT_OUTPUT_FILE))

        val releaseDependencies: ReleaseDependenciesExtension =
            objects.newInstance(ReleaseDependenciesExtension::class.java)

        val scan: ScanExtension = objects.newInstance(ScanExtension::class.java)

        fun releaseDependencies(action: Action<in ReleaseDependenciesExtension>) {
            action.execute(releaseDependencies)
        }

        fun releaseDependencies(block: ReleaseDependenciesExtension.() -> Unit) {
            releaseDependencies(Action { it.block() })
        }

        fun scan(action: Action<in ScanExtension>) {
            // Declaring a `scan { }` block enables scanning by default; an explicit
            // `enabled.set(false)` inside the block (or the gradle property) still wins.
            scan.enabled.convention(true)
            action.execute(scan)
        }

        fun scan(block: ScanExtension.() -> Unit) {
            scan(Action { it.block() })
        }

        companion object {
            const val DEFAULT_OUTPUT_FILE = "components-dependencies.json"
        }
    }
