package org.octopusden.octopus.build.integration.gradle.plugin.runner

import com.platformlib.process.api.ProcessInstance
import com.platformlib.process.builder.ProcessBuilder
import com.platformlib.process.factory.ProcessBuilders
import com.platformlib.process.local.specification.LocalProcessSpec
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

open class TestGradleDSL {
    lateinit var projectPath: String
    lateinit var gradleWrapperPath: String
    var additionalArguments: Array<String> = arrayOf()
    var additionalEnvVariables: Map<String, String> = mapOf()
    var tasks: Array<String> = arrayOf()
    var reuseExistingDir: Boolean = false
}

fun gradleProcessInstance(init: TestGradleDSL.() -> Unit): Pair<ProcessInstance, Path> {
    val spec = TestGradleDSL().apply(init)
    val tmpDir: Path = if (spec.reuseExistingDir) {
        val dir = Paths.get(spec.projectPath).toAbsolutePath()
        println("Reusing existing test project dir: $dir")
        if (!Files.isDirectory(dir)) {
            error("Reused project directory '$dir' does not exist or is not a directory")
        }
        dir
    } else {
        val projectPath = getResourcePath("/${spec.projectPath}", "Test project")
        if (!Files.isDirectory(projectPath)) {
            error("Project '${spec.projectPath}' not found at $projectPath")
        }
        val wrapperPath = getResourcePath("/${spec.gradleWrapperPath}", "Gradle Wrapper")
        if (!Files.isDirectory(wrapperPath)) {
            error("Wrapper '${spec.gradleWrapperPath}' not found at $wrapperPath")
        }
        val testsTmpRoot = Paths.get("").toAbsolutePath().resolve("build/tmp/test")
        val dir = Files.createTempDirectory(testsTmpRoot, "build-integration-test-").toAbsolutePath()
        println("Using temp test project dir: $dir")
        copy(projectPath, dir)
        copy(wrapperPath, dir)
        dir
    }
    val baseEnv = mapOf(
        "JAVA_HOME" to System.getProperty("java.home")
    )
    val processInstance = ProcessBuilders
        .newProcessBuilder<ProcessBuilder>(LocalProcessSpec.LOCAL_COMMAND)
        .envVariables(baseEnv + spec.additionalEnvVariables)
        .redirectStandardOutput(System.out)
        .redirectStandardError(System.err)
        .defaultExtensionMapping()
        .workDirectory(tmpDir)
        .processInstance { processInstanceConfiguration -> processInstanceConfiguration.unlimited() }
        .commandAndArguments(tmpDir.resolve("gradlew").toString(), "--no-daemon")
        .build()
        .execute(
            *(listOf(
                "-P$BUILD_INTEGRATION_VERSION_PROPERTY=${System.getProperty(BUILD_INTEGRATION_VERSION_PROPERTY)}"
            ) + spec.tasks + spec.additionalArguments).toTypedArray()
        )
        .toCompletableFuture()
        .join()

    return processInstance to tmpDir
}

private fun getResourcePath(path: String, description: String): Path {
    val resource = TestGradleDSL::class.java.getResource(path)
        ?: error("$description '$path' not found in resources")
    return Paths.get(resource.toURI())
}

private fun copy(from: Path, to: Path) {
    Files.walk(from).forEach { src ->
        val dest = to.resolve(from.relativize(src).toString())
        if (Files.isDirectory(src)) {
            if (!Files.exists(dest)) Files.createDirectories(dest)
        } else {
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
            if (src.fileName.toString() == "gradlew") {
                dest.toFile().setExecutable(true)
            }
        }
    }
}

private const val BUILD_INTEGRATION_VERSION_PROPERTY = "octopus-build-integration.version"