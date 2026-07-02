package org.jetbrains.compose.swing.test

import org.jetbrains.compose.swing.modifier.appearance.TEST_TAG_CLIENT_PROPERTY_KEY
import java.awt.Component
import java.awt.Container
import java.awt.Dialog
import java.awt.Frame
import javax.accessibility.AccessibleRole
import javax.swing.AbstractButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.text.JTextComponent

/**
 * A predicate over a single AWT [Component] together with a human-readable [description] used in
 * failure messages. Matchers are combined with [and] to narrow a query.
 *
 * Matching reads component state directly; callers are responsible for invoking matchers on the
 * EDT (the finder infrastructure does this).
 */
public class SwingMatcher internal constructor(
    public val description: String,
    private val predicate: (Component) -> Boolean,
) {
    /** Evaluates this matcher against [component]. Must be called on the EDT. */
    public fun matches(component: Component): Boolean = predicate(component)

    /** Returns a matcher satisfied only when both this and [other] match. */
    public infix fun and(other: SwingMatcher): SwingMatcher =
        SwingMatcher("($description && ${other.description})") {
            predicate(it) && other.matches(it)
        }

    public companion object {
        /**
         * Matches a component whose textual content equals [text], or contains it when [substring]
         * is `true`. Text is read from [JLabel.getText], [AbstractButton.getText], or
         * [JTextComponent.getText] depending on the component type.
         */
        public fun hasText(
            text: String,
            substring: Boolean = false,
        ): SwingMatcher {
            val desc = if (substring) "hasText(substring=\"$text\")" else "hasText(\"$text\")"
            return SwingMatcher(desc) { component ->
                val actual = component.textOrNull() ?: return@SwingMatcher false
                if (substring) actual.contains(text) else actual == text
            }
        }

        /** Matches a component whose [Component.getName] equals [name]. */
        public fun hasName(name: String): SwingMatcher = SwingMatcher("hasName(\"$name\")") { it.name == name }

        /** Matches a component tagged with [tag] via `SwingModifier.testTag`. */
        public fun hasTestTag(tag: String): SwingMatcher =
            SwingMatcher("hasTestTag(\"$tag\")") { component ->
                component is JComponent &&
                    component.getClientProperty(TEST_TAG_CLIENT_PROPERTY_KEY) == tag
            }

        /**
         * Matches a component whose accessible name equals [name], read from its
         * [java.awt.Component.getAccessibleContext].
         */
        public fun hasAccessibleName(name: String): SwingMatcher =
            SwingMatcher("hasAccessibleName(\"$name\")") { component ->
                component.accessibleContext?.accessibleName == name
            }

        /**
         * Matches a component whose accessible description equals [description], read from its
         * [java.awt.Component.getAccessibleContext].
         */
        public fun hasAccessibleDescription(description: String): SwingMatcher =
            SwingMatcher("hasAccessibleDescription(\"$description\")") { component ->
                component.accessibleContext?.accessibleDescription == description
            }

        /**
         * Matches a component whose accessible role equals [role], read from its
         * [java.awt.Component.getAccessibleContext].
         */
        public fun hasAccessibleRole(role: AccessibleRole): SwingMatcher =
            SwingMatcher("hasAccessibleRole($role)") { component ->
                component.accessibleContext?.accessibleRole == role
            }

        /**
         * Matches a top-level window whose title equals [title], read from [Frame.getTitle] or
         * [Dialog.getTitle]. Use with [SwingUiTest.onWindow] to pick one window out of several.
         */
        public fun hasTitle(title: String): SwingMatcher =
            SwingMatcher("hasTitle(\"$title\")") { component ->
                when (component) {
                    is Frame -> component.title == title
                    is Dialog -> component.title == title
                    else -> false
                }
            }

        /** Matches a component whose enabled state equals [enabled]. */
        public fun isEnabled(enabled: Boolean = true): SwingMatcher =
            SwingMatcher("isEnabled($enabled)") { it.isEnabled == enabled }

        /** Matches a component that is an instance of [T]. */
        public inline fun <reified T : Component> isOfType(): SwingMatcher = ofType(T::class.java)

        @PublishedApi
        internal fun ofType(type: Class<out Component>): SwingMatcher =
            SwingMatcher("isOfType(${type.simpleName})") { type.isInstance(it) }

        internal fun isRoot(root: Container): SwingMatcher = SwingMatcher("isRoot") { it === root }

        /** Matches every component; the identity for narrowing combinators. */
        internal fun any(): SwingMatcher = SwingMatcher("any") { true }
    }
}

/**
 * Returns the textual content of [this] component for matching purposes, or `null` if the component
 * type has no associated text.
 */
internal fun Component.textOrNull(): String? =
    when (this) {
        is JLabel -> text
        is AbstractButton -> text
        is JTextComponent -> text
        else -> null
    }

/**
 * Recursively collects every component in the subtree rooted at [this] (excluding [this] itself)
 * that satisfies [matcher], in depth-first pre-order. Must be called on the EDT.
 *
 * Walks the real AWT tree via [Container.getComponents].
 */
internal fun Container.findMatching(matcher: SwingMatcher): List<Component> {
    val results = mutableListOf<Component>()

    fun visit(container: Container) {
        for (child in container.components) {
            if (matcher.matches(child)) results += child
            if (child is Container) visit(child)
        }
    }
    visit(this)
    return results
}

/**
 * Collects [this] container (when it matches) and every matching descendant, in depth-first
 * pre-order. Must be called on the EDT.
 */
internal fun Container.findMatchingIncludingSelf(matcher: SwingMatcher): List<Component> {
    val self = if (matcher.matches(this)) listOf<Component>(this) else emptyList()
    return self + findMatching(matcher)
}

/**
 * Renders the subtree rooted at [this] as an indented, readable string for failure messages.
 * Must be called on the EDT.
 *
 * The dump is bounded so a deep or wide tree cannot flood a failure message: at most
 * [MAX_DUMP_DEPTH] levels deep and [MAX_DUMP_LINES] lines. Whatever is elided is replaced by a
 * single `(truncated …)` marker so the reader knows the structure continues, while the top of the
 * tree — the part that usually identifies the defect — is always preserved.
 */
internal fun Container.dumpTree(): String {
    val dump = BoundedTreeDump()
    dump.visit(this, depth = 0)
    return dump.finish()
}

/**
 * Accumulates an indented tree dump while enforcing the [MAX_DUMP_DEPTH] / [MAX_DUMP_LINES] bounds.
 */
private class BoundedTreeDump {
    private val sb = StringBuilder()
    private var lines = 0
    private var truncated = false

    fun visit(
        component: Component,
        depth: Int,
    ) {
        if (truncated) return
        if (lines >= MAX_DUMP_LINES) {
            truncated = true
            return
        }
        appendLine(depth, describe(component))
        val children = (component as? Container)?.components.orEmpty()
        when {
            children.isEmpty() -> Unit
            depth + 1 > MAX_DUMP_DEPTH -> appendLine(depth + 1, "(truncated: deeper levels omitted)")
            else -> for (child in children) visit(child, depth + 1)
        }
    }

    fun finish(): String {
        if (truncated) sb.append("(truncated: tree exceeds $MAX_DUMP_LINES lines)\n")
        return sb.toString()
    }

    private fun appendLine(
        depth: Int,
        text: String,
    ) {
        sb.append("  ".repeat(depth)).append(text).append('\n')
        lines++
    }

    private fun describe(component: Component): String {
        val type = component.javaClass.simpleName.ifEmpty { component.javaClass.name }
        val text = component.textOrNull()?.let { " text=\"$it\"" }.orEmpty()
        val name = component.name?.let { " name=\"$it\"" }.orEmpty()
        val context = component.accessibleContext
        val accessibleName = context?.accessibleName?.let { " a11yName=\"$it\"" }.orEmpty()
        val accessibleDescription = context?.accessibleDescription?.let { " a11yDesc=\"$it\"" }.orEmpty()
        val enabled = if (component.isEnabled) "" else " disabled"
        return "$type$text$name$accessibleName$accessibleDescription$enabled"
    }
}

// Bounds for [dumpTree]: keep the structurally useful top of the tree, drop the rest behind a
// "(truncated)" marker rather than flooding a failure message with thousands of lines.
private const val MAX_DUMP_DEPTH: Int = 4
private const val MAX_DUMP_LINES: Int = 100
