package org.jetbrains.compose.swing.foundation.layout

import org.jetbrains.compose.swing.components.InProcessCompilerHarness
import org.jetbrains.kotlin.cli.common.ExitCode
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins [ConstrainedScope] as the only scope in reach inside a [Layout]'s content. A child declares its
 * placement to the container that measures it, so a declaration meant for an enclosing row must not be
 * readable by a child of a `Layout` nested in one: the row's child is the `Layout`, and the policy the
 * `Layout` holds reads nothing a row's scope names.
 *
 * A caller cannot be shown a rejected program by a running test, so these compile one with the official
 * Kotlin compiler driven in-process and read the diagnostics it emits.
 */
class LayoutScopeCompilationTest {
    @Test
    fun aRowsDeclarationInsideANestedLayoutDoesNotCompile() {
        val result = compileSnippet("Label(\"Details\", modifier = SwingModifier.weight(1f))")

        assertEquals(
            ExitCode.COMPILATION_ERROR,
            result.exitCode,
            "a row's weight declared on a child of a nested layout must be rejected, output was:\n${result.output}",
        )
        assertTrue(
            result.errors().any { "weight" in it },
            "the rejection must name the declaration it is about, output was:\n${result.output}",
        )
    }

    /** What the policy itself honors stays in reach: those builders are the scope the content is given. */
    @Test
    fun aChildsOwnLayoutModifiersResolveInsideANestedLayout() {
        val result = compileSnippet("Label(\"Details\", modifier = SwingModifier.padding(4))")

        assertEquals(
            ExitCode.OK,
            result.exitCode,
            "a padded child of a nested layout must compile, output was:\n${result.output}",
        )
    }

    private companion object {
        /**
         * Resolves the compiler plugin classpath once at startup, so a test task that does not hand the
         * harness one reports a single failure here rather than the same failure in every case below.
         */
        @JvmStatic
        @BeforeAll
        fun verifyComposePluginClasspathAvailable() {
            InProcessCompilerHarness.resolveComposePluginClasspath()
        }

        /** [child] written inside a `Layout` that is itself written inside a `Row`. */
        fun compileSnippet(child: String) = InProcessCompilerHarness.compileSnippet(
            "LayoutScopeSnippet.kt",
            """
            import org.jetbrains.compose.swing.components.Label
            import org.jetbrains.compose.swing.foundation.layout.Constraints
            import org.jetbrains.compose.swing.foundation.layout.Layout
            import org.jetbrains.compose.swing.foundation.layout.Row
            import org.jetbrains.compose.swing.modifier.SwingModifier

            @androidx.compose.runtime.Composable
            fun Nested() {
                Row {
                    Layout({ measurables, constraints ->
                        val placeables = measurables.map { it.measure(Constraints(maxWidth = constraints.maxWidth)) }
                        layout(placeables.maxOfOrNull { it.width } ?: 0, placeables.sumOf { it.height }) {
                            var y = 0
                            for (placeable in placeables) {
                                placeable.placeRelative(0, y)
                                y += placeable.height
                            }
                        }
                    }) {
                        $child
                    }
                }
            }
            """.trimIndent(),
        )
    }
}
