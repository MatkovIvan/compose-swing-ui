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
    ) {
        /**
         * What the compiler said in each error diagnostic, with the source location it prefixes the
         * message with dropped.
         *
         * The location is the snippet's own file path, so a match against the whole line would hold for
         * whatever the snippet file happens to be called rather than for anything the compiler found in
         * it: the name of the declaration under test appears in every diagnostic, and in a passing
         * assertion, even when the compiler is complaining about something else entirely.
         */
        fun errors(): List<String> = output
            .lineSequence()
            .mapNotNull { line ->
                val separator = ERROR_DIAGNOSTIC_MARKER.find(line) ?: return@mapNotNull null
                line.substring(separator.range.last + 1)
            }.toList()
    }

    /**
     * Compiles [source] with the Compose compiler plugin loaded, returning the exit code and the
     * captured diagnostic stream (one `w:` / `e:` prefixed line per diagnostic).
     */
    internal fun compileSnippet(
        relativePath: String,
        source: String,
    ): CompilationResult = compileSnippet(SourceSpec(relativePath, source), composePluginClasspath)

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

    /**
     * The severity marker the compiler's textual renderer puts between a diagnostic location and its
     * message. Whitespace is intentionally not part of the contract: it is presentation, while the
     * marker itself is what identifies an error line. The raw stream remains available through
     * [CompilationResult.output] for diagnostics that need the exact compiler rendering.
     */
    private val ERROR_DIAGNOSTIC_MARKER = Regex(""":\s*error:\s*""")

    /** The system property the Gradle test task uses to hand the harness the plugin jar(s). */
    private const val PLUGIN_CLASSPATH_PROPERTY = "compose.compiler.plugin.classpath"

    /**
     * The Compose compiler plugin jar(s) the Gradle test task hands every test in this module, so a
     * snippet is compiled the way the real build compiles it. Whether a call has a receiver to resolve
     * against is settled by the frontend either way.
     *
     * A suite whose snippets all compile without the plugin asserts over a compilation that is not the
     * one the real build performs, so resolving fails loudly rather than falling back to no plugin.
     */
    internal val composePluginClasspath: List<File> by lazy { resolveComposePluginClasspath() }

    /**
     * Resolves the Compose compiler plugin jar(s) from the [PLUGIN_CLASSPATH_PROPERTY] system property,
     * asserting both that the property is set and that every jar it names exists, with a message that
     * tells the user exactly which property the Gradle test task must set.
     */
    internal fun resolveComposePluginClasspath(): List<File> {
        val raw =
            System.getProperty(PLUGIN_CLASSPATH_PROPERTY)
                ?: throw AssertionError(
                    "System property '$PLUGIN_CLASSPATH_PROPERTY' is not set; the Gradle test task must " +
                        "hand the resolved Compose compiler plugin jar to the harness. Run these tests " +
                        "via Gradle (./gradlew :swing-ui:test), or set " +
                        "-D$PLUGIN_CLASSPATH_PROPERTY=<path-to-compose-compiler-plugin.jar> when running " +
                        "them directly.",
                )
        val jars = raw.split(File.pathSeparator).filter(String::isNotBlank).map(::File)
        val missing = jars.filterNot(File::exists)
        if (jars.isEmpty() || missing.isNotEmpty()) {
            throw AssertionError(
                "System property '$PLUGIN_CLASSPATH_PROPERTY' does not point at existing Compose " +
                    "compiler plugin jar(s). Value: '$raw'. " +
                    if (jars.isEmpty()) {
                        "No jar paths were listed."
                    } else {
                        "Missing: ${missing.joinToString { it.path }}."
                    },
            )
        }
        return jars
    }
}
