package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.octopusden.octopus.build.integration.gradle.plugin.model.ComponentSelector

class ComponentsBuilder {

    internal val components = mutableListOf<ComponentSelector>()

    fun include(id: String, version: String) {
        components += ComponentSelector(id, version)
    }

}