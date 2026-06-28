package org.jetbrains.compose.swing.test.interaction

import org.jetbrains.compose.swing.test.SwingMatcher
import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.dumpTree
import org.jetbrains.compose.swing.test.findMatching
import org.jetbrains.compose.swing.test.textOrNull
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.AbstractButton
import javax.swing.text.JTextComponent

/**
 * A lazy handle to the single component matched by a query. The match is resolved against the live
 * AWT tree each time it is needed, so it always reflects the current tree after recomposition.
 *
 * All methods are intended to be called from a [runSwingUiTest] body, which runs on the EDT.
 * Resolution fails with a readable tree dump when zero or more than one component matches.
 */
public class SwingNodeInteraction internal constructor(
    private val test: SwingUiTest,
    private val matcher: SwingMatcher,
    private val description: String,
) {
    private fun resolveOrNull(): Component? {
        val matches = collectMatches()
        return when (matches.size) {
            0 -> null

            1 -> matches.single()

            else -> throw AssertionError(
                "Expected exactly one node matching '$description' but found ${matches.size}.\n" +
                    "Tree:\n${test.root.dumpTree()}",
            )
        }
    }

    /** Resolves the single matching component, failing with a tree dump if not exactly one matches. */
    @PublishedApi
    internal fun resolve(): Component {
        val matches = collectMatches()
        return when (matches.size) {
            1 -> matches.single()

            0 -> throw AssertionError(
                "Expected exactly one node matching '$description' but found none.\n" +
                    "Tree:\n${test.root.dumpTree()}",
            )

            else -> throw AssertionError(
                "Expected exactly one node matching '$description' but found ${matches.size}.\n" +
                    "Tree:\n${test.root.dumpTree()}",
            )
        }
    }

    private fun collectMatches(): List<Component> {
        val root = test.root
        val all = root.findMatching(matcher)
        // onRoot() targets the root itself, which findMatching (children only) never returns.
        return if (matcher.matches(root)) listOf<Component>(root) + all else all
    }

    /**
     * Resolves the matched component and returns it typed as [T], for driving a component's own API
     * directly (e.g. a `JTable`'s model, a `JTree`'s selection, a `JList`'s model).
     *
     * @throws AssertionError if no single node matches, or if the matched node is not a [T].
     */
    public inline fun <reified T : Component> fetch(): T {
        val component = resolve()
        if (component !is T) {
            throw AssertionError(
                "Node '$matcherDescription' is a ${component.javaClass.simpleName}, " +
                    "expected a ${T::class.java.simpleName}.",
            )
        }
        return component
    }

    /** Human-readable description of this interaction's matcher, for [fetch] failure messages. */
    @PublishedApi
    internal val matcherDescription: String
        get() = description

    /** The owning test, for extensions that delegate to the [SwingUiTest]-level assertions. */
    internal val owningTest: SwingUiTest
        get() = test

    // region assertions

    /** Asserts that exactly one node matches. Returns this interaction for chaining. */
    public fun assertExists(): SwingNodeInteraction {
        resolve()
        return this
    }

    /** Asserts that no node matches. */
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
     * Asserts the matched node is displayed, with **headless semantics**.
     *
     * The test harness runs off-screen (`-Djava.awt.headless=true`) and never realizes a native
     * window, so [java.awt.Component.isShowing] is permanently `false` and cannot be used. Instead,
     * "displayed" here means the node is genuinely part of the laid-out tree under the test [root]:
     *
     *  1. it is **attached** — reachable from [root] by walking parents (so an orphaned/removed
     *     component fails even if it still reports stale bounds), and
     *  2. it has **non-zero bounds** produced by the forced layout pass the harness runs, i.e. a real
     *     width and height assigned by its ancestor's layout manager.
     *
     * This catches the failures a visibility check is meant to catch off-screen — a node that was
     * detached, or one that a layout collapsed to zero size — without requiring an on-screen peer.
     */
    public fun assertIsDisplayed(): SwingNodeInteraction {
        val component = resolve()
        if (!component.isAttachedTo(test.root)) {
            throw AssertionError(
                "Node '$description' is not displayed: it is not attached under the test root.\n" +
                    "Tree:\n${test.root.dumpTree()}",
            )
        }
        if (component.width <= 0 || component.height <= 0) {
            throw AssertionError(
                "Node '$description' is not displayed: it has zero laid-out size " +
                    "(${component.width}x${component.height}). The forced layout pass assigned " +
                    "it no bounds within its ancestor.\nTree:\n${test.root.dumpTree()}",
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
