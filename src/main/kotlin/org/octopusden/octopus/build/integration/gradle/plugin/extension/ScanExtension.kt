package org.octopusden.octopus.build.integration.gradle.plugin.extension

class ScanExtension {

    private var enabled = false
    private var componentsRegistryUrl = ""
    private var projects = ".+"
    private var configurations = "runtime.+"

    fun setEnabled(value: Boolean) { enabled = value }
    fun setComponentsRegistryUrl(value: String) { componentsRegistryUrl = value }
    fun setProjects(value: String) { projects = value }
    fun setConfigurations(value: String) { configurations = value }

    fun isEnabled() = enabled
    fun getComponentsRegistryUrl() = componentsRegistryUrl
    fun getProjects() = projects
    fun getConfigurations() = configurations
}