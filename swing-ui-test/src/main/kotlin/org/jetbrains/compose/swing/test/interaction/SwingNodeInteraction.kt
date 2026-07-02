package org.jetbrains.compose.swing.test.interaction

import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.dumpTree
import org.jetbrains.compose.swing.test.textOrNull
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import javax.swing.AbstractButton
import javax.swing.text.JTextComponent

/**
 * A lazy handle to the single component targeted by a query. The target is resolved against the
 * live AWT tree each time it is needed, so it always reflects the current tree after recomposition.
 *
 * All methods are intended to be called from a [org.jetbrains.compose.swing.test.runSwingUiTest]
 * body, which runs on the EDT.
 * Resolution fails with a readable tree dump when the query does not resolve to a single component.
 */
public class SwingNodeInteraction internal constructor(
    private val test: SwingUiTest,
    private val description: String,
    private val root: () -> Container,
    private val pick: NodePick,
    private val candidates: () -> List<Component>,
) {
    private fun resolveOrNull(): Component? {
        val matches = candidates()
        if (pick == NodePick.Single && matches.size > 1) {
            throw AssertionError("${singleMatchMessage(matches.size)}\nTree:\n${root().dumpTree()}")
        }
        return selectTarget(matches)
    }

    /** Resolves the single targeted component, failing with a tree dump if the query has no target. */
    @PublishedApi
    internal fun resolve(): Component {
        val matches = candidates()
        return selectTarget(matches)
            ?: throw AssertionError("${noTargetMessage(matches.size)}\nTree:\n${root().dumpTree()}")
    }

    /** The single component this query's [pick] selects among [matches], or null when none is selected. */
    private fun selectTarget(matches: List<Component>): Component? =
        when (pick) {
            NodePick.Single -> matches.singleOrNull()
            is NodePick.AtIndex -> matches.getOrNull(pick.index)
            NodePick.Last -> matches.lastOrNull()
        }

    /** Why [resolve] found no target among [matched] matches, phrased for this query's [pick]. */
    private fun noTargetMessage(matched: Int): String =
        when (pick) {
            NodePick.Single -> {
                singleMatchMessage(matched)
            }

            is NodePick.AtIndex -> {
                "Expected a node at '$description' but only $matched node(s) matched."
            }

            NodePick.Last -> {
                "Expected a node at '$description' but none matched."
            }
        }

    /** The single source of the "expected exactly one node" message, for both ambiguity and no match. */
    private fun singleMatchMessage(matched: Int): String =
        "Expected exactly one node matching '$description' but found " +
            "${if (matched == 0) "none" else "$matched"}."

    /**
     * Resolves the matched component and returns it typed as [T], for driving a component's own API
     * directly (e.g. a `JTable`'s model, a `JTree`'s selection, a `JList`'s model).
     *
     * @throws AssertionError if the query has no single target, or if the target is not a [T].
     */
    public inline fun <reified T : Component> fetch(): T = resolve().castOrFail("Node", matcherDescription)

    /** Human-readable description of this interaction's query, for [fetch] failure messages. */
    @PublishedApi
    internal val matcherDescription: String
        get() = description

    /** The owning test, for extensions that delegate to the [SwingUiTest]-level assertions. */
    internal val owningTest: SwingUiTest
        get() = test

    // region assertions

    /** Asserts that the query resolves to a node. Returns this interaction for chaining. */
    public fun assertExists(): SwingNodeInteraction {
        resolve()
        return this
    }

    /** Asserts that the query resolves to no node. */
    public fun assertDoesNotExist() {
        val match = resolveOrNull()
        if (match != null) {
            throw AssertionError(
                "Expected no node matching '$description' but found one:\n" +
                    match.javaClass.simpleName + " text=\"" + match.textOrNull() + "\"",
            )
        }
    }

    /** Asserts the matched node's text equals [expected]. */
    public fun assertTextEquals(expected: String): SwingNodeInteraction {
        val actual = resolve().textOrNull()
        if (actual != expected) {
            throw AssertionError(
                "Node '$description' text was ${actual?.let { "\"$it\"" } ?: "null"}, " +
                    "expected \"$expected\".",
            )
        }
        return this
    }

    /** Asserts the matched node is enabled. */
    public fun assertIsEnabled(): SwingNodeInteraction = assertEnabledState(true)

    /** Asserts the matched node is not enabled. */
    public fun assertIsNotEnabled(): SwingNodeInteraction = assertEnabledState(false)

    private fun assertEnabledState(expected: Boolean): SwingNodeInteraction {
        val actual = resolve().isEnabled
        if (actual != expected) {
            throw AssertionError(
                "Node '$description' was ${if (actual) "enabled" else "disabled"}, " +
                    "expected ${if (expected) "enabled" else "disabled"}.",
            )
        }
        return this
    }

    /**
     * Asserts the matched node is displayed, with **off-screen semantics**.
     *
     * The test harness never attaches its root to a window, so no native peer is realized (with or
     * without a display), [java.awt.Component.isShowing] is permanently `false`, and it cannot be
     * used. Instead, "displayed" here means the node is genuinely part of the laid-out tree under
     * the query's root — the harness root, or the window's content pane for a window-scoped query:
     *
     *  1. it is **attached** — reachable from the query's root by walking parents (so an
     *     orphaned/removed component fails even if it still reports stale bounds), and
     *  2. it has **non-zero bounds** produced by the forced layout pass the harness runs, i.e. a real
     *     width and height assigned by its ancestor's layout manager.
     *
     * This catches the failures a visibility check is meant to catch off-screen — a node that was
     * detached, or one that a layout collapsed to zero size — without requiring an on-screen peer.
     */
    public fun assertIsDisplayed(): SwingNodeInteraction {
        val component = resolve()
        val currentRoot = root()
        if (!component.isAttachedTo(currentRoot)) {
            throw AssertionError(
                "Node '$description' is not displayed: it is not attached under the query root.\n" +
                    "Tree:\n${currentRoot.dumpTree()}",
            )
        }
        if (component.width <= 0 || component.height <= 0) {
            throw AssertionError(
                "Node '$description' is not displayed: it has zero laid-out size " +
                    "(${component.width}x${component.height}). The forced layout pass assigned " +
                    "it no bounds within its ancestor.\nTree:\n${currentRoot.dumpTree()}",
            )
        }
        return this
    }

    /** Walks parents from [this] up to (and including) [ancestor], returning true if reached. */
    private fun Component.isAttachedTo(ancestor: Component): Boolean {
        var current: Component? = this
        while (current != null) {
            if (current === ancestor) return true
            current = current.parent
        }
        return false
    }

    /**
     * Asserts the matched node was added to its parent with layout constraint [expected].
     *
     * Reads the constraint from the parent's [BorderLayout] via [BorderLayout.getConstraints]; the
     * node's parent must use a [BorderLayout] (e.g. it is a slot child of a `BorderPanel`).
     */
    public fun assertLayoutConstraint(expected: Any): SwingNodeInteraction {
        layoutConstraintFailure(resolve(), expected)?.let { throw AssertionError(it) }
        return this
    }

    /** Returns the reason [component]'s layout constraint does not match [expected], or null if it does. */
    private fun layoutConstraintFailure(
        component: Component,
        expected: Any,
    ): String? {
        val parent = component.parent
        val layout = parent?.layout
        return when {
            parent == null -> {
                "Node '$description' has no parent container."
            }

            layout !is BorderLayout -> {
                "Node '$description' parent uses ${layout?.javaClass?.simpleName}, " +
                    "not a BorderLayout; cannot read a layout constraint."
            }

            layout.getConstraints(component) != expected -> {
                "Node '$description' layout constraint was " +
                    "${layout.getConstraints(component) ?: "null"}, expected $expected."
            }

            else -> {
                null
            }
        }
    }

    // endregion

    // region actions

    /**
     * Clicks the matched node and settles the composition. The node must be an [AbstractButton]
     * (Button, CheckBox, RadioButton, etc.); [AbstractButton.doClick] is invoked, firing registered
     * action listeners.
     */
    public suspend fun performClick(): SwingNodeInteraction {
        val component = resolve()
        if (component !is AbstractButton) {
            throw AssertionError(
                "Node '$description' is a ${component.javaClass.simpleName}, " +
                    "which cannot be clicked (expected an AbstractButton).",
            )
        }
        component.doClick()
        test.awaitIdle()
        return this
    }

    /**
     * Appends [text] to the matched [JTextComponent]'s current content, as if typed at its end, then
     * settles the composition.
     */
    public suspend fun performTextInput(text: String): SwingNodeInteraction {
        editText { it.text + text }
        return this
    }

    /**
     * Replaces the matched [JTextComponent]'s entire content with [text], then settles the
     * composition.
     */
    public suspend fun performTextReplacement(text: String): SwingNodeInteraction {
        editText { text }
        return this
    }

    private suspend fun editText(transform: (JTextComponent) -> String) {
        val component = resolve()
        if (component !is JTextComponent) {
            throw AssertionError(
                "Node '$description' is a ${component.javaClass.simpleName}, " +
                    "which cannot receive text input (expected a JTextComponent).",
            )
        }
        component.text = transform(component)
        test.awaitIdle()
    }

    // endregion
}

/** How a [SwingNodeInteraction] selects its target among the query's matches. */
internal sealed interface NodePick {
    /** The query must match exactly one node. */
    data object Single : NodePick

    /** The query targets the match at [index], in depth-first pre-order; other matches may exist. */
    data class AtIndex(
        val index: Int,
    ) : NodePick

    /** The query targets the last match, re-resolved on every use. */
    data object Last : NodePick
}
