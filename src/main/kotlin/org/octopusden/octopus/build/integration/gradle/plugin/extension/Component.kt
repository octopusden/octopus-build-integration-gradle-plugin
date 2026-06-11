package org.octopusden.octopus.build.integration.gradle.plugin.extension

import java.io.Serializable

data class Component(
    val name: String,
    val version: String
) : Serializable