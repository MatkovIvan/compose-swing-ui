package org.jetbrains.compose.swing.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.BorderLayout
import java.awt.Component
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Regression guard for the original showcase bug, exercised through a genuine **structural** change
 * that shifts sibling indices — the only thing that actually triggered the historical applier defect.
 *
 * Layout: a [BorderPanel] showcase with a stable NORTH title and a stable SOUTH status bar, plus a
 * CENTER body section that conditionally adds/removes a child (a [Button]). The BorderPanel composes
 * its slots in a fixed order — NORTH, then CENTER, then SOUTH — so the CENTER child, when present,
 * occupies composition index 1, *between* the title and the status bar.
 *
 * Why this reproduces the bug (and the previous counter-only test did not): the old applier added a
 * constrained child with `Container.add(Component, Object)`, which appends to the AWT component array
 * and ignores the composition index. With NORTH and SOUTH already present, toggling the CENTER body
 * ON inserted it at composition index 1 but the buggy applier *appended* it (array order became
 * [title, status, body] while composition order was [title, body, status]). Toggling the body back
 * OFF then issued an index-based remove at composition index 1, which on the desynced array removed
 * the SOUTH status bar instead of the CENTER body. A test that merely mutates a counter performs no
 * structural insert/remove, so it never desyncs the array and passes against the buggy applier too.
 *
 * The fixed applier inserts constrained children at their composition index via
 * `add(Component, Object, int)`, keeping array order == composition order, so every sibling survives.
 */
class ShowcaseLabelRegressionTest {
    private companion object {
        const val TITLE = "Compose Swing UI - Component Showcase"
        const val STATUS = "Ready"
        const val BODY = "Run"
    }

    /** Resolves the single live component with [text] by walking the real AWT tree on the EDT. */
    private fun SwingUiTest.componentWithText(text: String): Component = onNodeWithText(text).fetch<Component>()

    @Test
    fun titleInNorthSurvivesStructuralChurnInCenterBody() = runSwingUiTest {
        var showBody by mutableStateOf(false)
        var clicks by mutableIntStateOf(0)
        setContent {
            BorderPanel {
                north { Label(text = TITLE) }
                if (showBody) {
                    center { Button(text = BODY, onClick = { clicks++ }) }
                }
                south { Label(text = STATUS) }
            }
        }

        // Baseline: title and status bar present in their regions; body absent.
        onNodeWithText(TITLE)
            .assertExists()
            .assertLayoutConstraint(BorderLayout.NORTH)
            .assertIsDisplayed()
        onNodeWithText(STATUS).assertExists().assertLayoutConstraint(BorderLayout.SOUTH)
        onNodeWithText(BODY).assertDoesNotExist()

        // Capture the live instances so we can prove identity survives the structural toggle cycle.
        val titleBefore = componentWithText(TITLE)
        val statusBefore = componentWithText(STATUS)

        // Add the CENTER body. With the buggy applier this constrained insert at composition index 1
        // was appended past SOUTH, desyncing the array.
        showBody = true
        awaitIdle()
        onNodeWithText(BODY).assertExists().assertLayoutConstraint(BorderLayout.CENTER)

        // Remove the CENTER body. The buggy applier's index-based remove hit SOUTH on the desynced
        // array, deleting the status bar instead of the body.
        showBody = false
        awaitIdle()

        // The body is gone, but the unrelated NORTH title and SOUTH status bar must be intact:
        // same existence, same regions, same instances, and the title is the surviving NORTH node.
        onNodeWithText(BODY).assertDoesNotExist()
        onNodeWithText(TITLE)
            .assertExists()
            .assertLayoutConstraint(BorderLayout.NORTH)
            .assertIsDisplayed()
        onNodeWithText(STATUS)
            .assertExists()
            .assertLayoutConstraint(BorderLayout.SOUTH)
        assertSame(titleBefore, componentWithText(TITLE), "NORTH title instance changed across churn")
        assertSame(statusBefore, componentWithText(STATUS), "SOUTH status instance changed across churn")
    }

    @Test
    fun repeatedBodyTogglesKeepTitleAndStatusStable() = runSwingUiTest {
        var showBody by mutableStateOf(false)
        setContent {
            BorderPanel {
                north { Label(text = TITLE) }
                if (showBody) {
                    center { Label(text = BODY) }
                }
                south { Label(text = STATUS) }
            }
        }

        val titleBefore = componentWithText(TITLE)
        val statusBefore = componentWithText(STATUS)

        repeat(5) {
            showBody = true
            awaitIdle()
            onNodeWithText(BODY).assertExists().assertLayoutConstraint(BorderLayout.CENTER)

            showBody = false
            awaitIdle()
            onNodeWithText(BODY).assertDoesNotExist()
        }

        onNodeWithText(TITLE).assertExists().assertLayoutConstraint(BorderLayout.NORTH).assertIsDisplayed()
        onNodeWithText(STATUS).assertExists().assertLayoutConstraint(BorderLayout.SOUTH)
        assertSame(titleBefore, componentWithText(TITLE), "NORTH title instance changed across churn")
        assertSame(statusBefore, componentWithText(STATUS), "SOUTH status instance changed across churn")
    }
}
