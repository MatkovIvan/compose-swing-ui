package org.jetbrains.compose.swing.components

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.createTempDirectory

/**
 * Lightweight in-process Kotlin compiler harness that runs the official embeddable Kotlin compiler
 * against a single Kotlin snippet, with the Compose compiler plugin loaded, and captures the emitted
 * diagnostics. It avoids spawning a nested Gradle build and inherits the test classpath so the snippet
 * sees the library's composables and the Compose runtime.
 */
internal object InProcessCompilerHarness {
    /** A Kotlin source to compile, addressed by its file name within the temporary source root. */
    internal data class SourceSpec(
        val relativePath: String,
        val contents: String,
    )

    /** The outcome of one compilation: the compiler's [exitCode] and its raw diagnostic [output]. */
    internal data class CompilationResult(
        val exitCode: ExitCode,
        val output: String,
    )

    /**
     * Compiles [source] with the Compose compiler plugin(s) at [pluginClasspath] loaded, returning the
     * exit code and the captured diagnostic stream (one `w:` / `e:` prefixed line per diagnostic).
     */
    internal fun compileSnippet(
        source: SourceSpec,
        pluginClasspath: List<File>,
    ): CompilationResult {
        val projectDir = createTempDirectory(prefix = "swing-ui-inprocess-compiler").toFile()
        val sourceRoot = projectDir.resolve("src").apply(File::mkdirs)
        val classesDir = projectDir.resolve("classes").apply(File::mkdirs)

        val sourceFile = sourceRoot.resolve(source.relativePath)
        sourceFile.parentFile.mkdirs()
        sourceFile.writeText(source.contents)

        val compilerOutput = ByteArrayOutputStream()
        return PrintStream(compilerOutput, true, Charsets.UTF_8.name()).use { output ->
            val args = buildCompilerArgs(classesDir, sourceFile, pluginClasspath)
            val exitCode = K2JVMCompiler().exec(output, *args)
            CompilationResult(exitCode, compilerOutput.toString(Charsets.UTF_8))
        }
    }

    private fun buildCompilerArgs(
        classesDir: File,
        sourceFile: File,
        pluginClasspath: List<File>,
    ): Array<String> = buildList {
        add("-d")
        add(classesDir.absolutePath)
        add("-module-name")
        add("swing-ui-target-mismatch-test")
        // Inherit the test classpath so the snippet resolves the library composables and the Compose
        // runtime exactly as the real build does.
        add("-classpath")
        add(System.getProperty("java.class.path").orEmpty())
        add("-no-stdlib")
        add("-no-reflect")
        add("-jvm-target")
        add("11")
        pluginClasspath.forEach { jar -> add("-Xplugin=${jar.absolutePath}") }
        add(sourceFile.absolutePath)
    }.toTypedArray()
}
