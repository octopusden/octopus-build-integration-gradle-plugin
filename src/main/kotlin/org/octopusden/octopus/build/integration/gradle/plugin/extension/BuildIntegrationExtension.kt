package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class BuildIntegrationExtension @Inject constructor(
    objects: ObjectFactory
) {

    val dependenciesExtension: DependenciesExtension = objects.newInstance(DependenciesExtension::class.java)

    fun dependencies(action: Action<in DependenciesExtension>) {
        action.execute(dependenciesExtension)
    }

    fun dependencies(block: DependenciesExtension.() -> Unit) {
        dependencies(Action { it.block() })
    }

}