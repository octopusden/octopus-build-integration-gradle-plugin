package org.octopusden.octopus.build.integration.gradle.plugin.extension

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ReleaseDependenciesExtensionTest {
    private val extension = ProjectBuilder
        .builder()
        .build()
        .objects
        .newInstance(ReleaseDependenciesExtension::class.java)

    @Test
    fun `string form rejects more than two segments`() {
        val ex = assertThrows<GradleException> { extension.component("a:b:c") }
        assertEquals("Incorrect component format for a:b:c. Should be 'componentName:version'", ex.message)
    }

    @Test
    fun `map form requires version`() {
        val ex = assertThrows<GradleException> { extension.component(mapOf("name" to "deployer")) }
        assertEquals("Incorrect component declaration {name=deployer}. 'version' is required", ex.message)
    }

    @Test
    fun `blank name is rejected`() {
        val ex = assertThrows<GradleException> { extension.component(mapOf("name" to "  ", "version" to "1.0.0")) }
        assertEquals("Incorrect component declaration: 'name' must not be blank", ex.message)
    }

    @Test
    fun `blank version is rejected`() {
        val ex = assertThrows<GradleException> { extension.component("deployer: ") }
        assertEquals("Incorrect component declaration: 'version' must not be blank", ex.message)
    }

    @Test
    fun `valid forms are accepted and trimmed`() {
        extension.component(mapOf("name" to "a", "version" to "1.0.0"))
        extension.component(" b : 2.0.0 ")
        extension.component("c", "3.0.0")
        assertEquals(
            setOf(
                Component("a", "1.0.0"),
                Component("b", "2.0.0"),
                Component("c", "3.0.0"),
            ),
            extension.components.get(),
        )
    }
}
