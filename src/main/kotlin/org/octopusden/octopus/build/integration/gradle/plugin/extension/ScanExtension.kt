package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ScanExtension @Inject constructor(
    objects: ObjectFactory
) {

    val enabled: Property<Boolean> = objects.property(Boolean::class.java)

    val componentsRegistryUrl: Property<String> = objects.property(String::class.java)

    val projects: Property<String> = objects.property(String::class.java)

    val configurations: Property<String> = objects.property(String::class.java)

}