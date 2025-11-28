package org.octopusden.octopus.build.integration.gradle.plugin.extension

class ScanExtension {

    private var enabled: Boolean? = null
    private var componentsRegistryUrl: String? = null
    private var projects: String? = null
    private var configurations: String? = null

    fun setEnabled(value: Boolean) { enabled = value }
    fun setComponentsRegistryUrl(value: String) { componentsRegistryUrl = value }
    fun setProjects(value: String) { projects = value }
    fun setConfigurations(value: String) { configurations = value }

    fun isEnabled() = enabled
    fun getComponentsRegistryUrl() = componentsRegistryUrl
    fun getProjects() = projects
    fun getConfigurations() = configurations
}