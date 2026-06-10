package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

abstract class ReleaseDependenciesExtension @Inject constructor(
    objects: ObjectFactory
) {

    val components: SetProperty<Component> = objects.setProperty(Component::class.java)

    /**
     * Groovy named-argument form: `component name: "deployer", version: project.'deployer.version'`.
     */
    fun component(declaration: Map<String, *>) {
        val name = declaration["name"]?.toString()
            ?: throw GradleException("Incorrect component declaration $declaration. 'name' is required")
        val version = declaration["version"]?.toString()
            ?: throw GradleException("Incorrect component declaration $declaration. 'version' is required")
        addComponent(name, version)
    }

    /**
     * Single-string form: `component "deployer:1.0.0"`.
     */
    fun component(declaration: String) {
        val items = declaration.split(":")
        if (items.size != 2) {
            throw GradleException("Incorrect component format for $declaration. Should be 'componentName:version'")
        }
        addComponent(items[0], items[1])
    }

    /**
     * Explicit form, convenient for the Kotlin DSL: `component("deployer", "1.0.0")`.
     */
    fun component(name: String, version: String) {
        addComponent(name, version)
    }

    private fun addComponent(name: String, version: String) {
        val trimmedName = name.trim()
        val trimmedVersion = version.trim()
        if (trimmedName.isEmpty()) {
            throw GradleException("Incorrect component declaration: 'name' must not be blank")
        }
        if (trimmedVersion.isEmpty()) {
            throw GradleException("Incorrect component declaration: 'version' must not be blank")
        }
        components.add(Component(trimmedName, trimmedVersion))
    }
}