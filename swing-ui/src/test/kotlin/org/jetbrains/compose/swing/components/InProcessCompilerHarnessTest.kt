package org.jetbrains.compose.swing.components

import org.jetbrains.compose.swing.components.InProcessCompilerHarness.CompilationResult
import org.jetbrains.kotlin.cli.common.ExitCode
import kotlin.test.Test
import kotlin.test.assertEquals

/** Focused tests for the harness's compiler-output parsing, independent of a compiler invocation. */
class InProcessCompilerHarnessTest {
    @Test
    fun errorsExtractMessagesWithoutDependingOnDiagnosticSpacing() {
        val output =
            """
            e: /tmp/Snippet.kt:3:9: error: first diagnostic
            e: /tmp/Snippet.kt:4:9: warning: ignored diagnostic
            e: /tmp/Snippet.kt:5:9:error: second diagnostic
            """.trimIndent()

        val result = CompilationResult(ExitCode.COMPILATION_ERROR, output)

        assertEquals(
            listOf("first diagnostic", "second diagnostic"),
            result.errors(),
            "only error diagnostics should be returned, without coupling to renderer spacing",
        )
        assertEquals(output, result.output, "raw compiler output must remain available unchanged")
    }
}
