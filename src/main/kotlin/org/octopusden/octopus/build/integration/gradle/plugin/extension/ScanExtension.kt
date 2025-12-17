package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ScanExtension @Inject constructor(
    objects: ObjectFactory
) {

    val enabled: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(DEFAULT_ENABLED)

    val componentsRegistryUrl: Property<String> = objects.property(String::class.java)

    val projects: Property<String> = objects.property(String::class.java)
        .convention(DEFAULT_PROJECTS)

    val configurations: Property<String> = objects.property(String::class.java)
        .convention(DEFAULT_CONFIGURATIONS)

    companion object {
        const val DEFAULT_ENABLED = false
        const val DEFAULT_PROJECTS = ".+"
        const val DEFAULT_CONFIGURATIONS = "runtime.+"
    }
}