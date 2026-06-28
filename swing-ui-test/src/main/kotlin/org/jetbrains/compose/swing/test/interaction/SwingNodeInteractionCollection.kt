package org.jetbrains.compose.swing.test.interaction

import org.jetbrains.compose.swing.test.SwingMatcher
import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.dumpTree
import org.jetbrains.compose.swing.test.findMatching
import java.awt.Component
import javax.swing.SwingUtilities

/**
 * A lazy handle to the set of components matched by a query. The match set is resolved against the
 * live AWT tree each time it is needed, so it reflects the current tree.
 *
 * All methods are intended to be called from a [runSwingUiTest] body, which runs on the EDT.
 */
public class SwingNodeInteractionCollection internal constructor(
    private val test: SwingUiTest,
    private val matcher: SwingMatcher,
    private val ancestor: Component? = null,
) {
    @PublishedApi
    internal fun resolveAll(): List<Component> {
        val matches = test.root.findMatching(matcher)
        val scope = ancestor ?: return matches
        return matches.filter { it !== scope && SwingUtilities.isDescendingFrom(it, scope) }
    }

    /**
     * Narrows this collection to only the matches that are descendants of [ancestor] (at any depth),
     * scoping a tree-wide query to a single subtree. The results are the descendants of [ancestor],
     * excluding [ancestor] itself. Returns a new collection; the original is unchanged.
     *
     * ```
     * val panel = onNodeWithTag("editor").fetch<JComponent>()
     * onAllNodesOfType<JLabel>().within(panel).fetchAll<JLabel>()
     * ```
     */
    public fun within(ancestor: Component): SwingNodeInteractionCollection =
        SwingNodeInteractionCollection(test, matcher, ancestor)

    /** Asserts that exactly [expected] nodes match. */
    public fun assertCountEquals(expected: Int): SwingNodeInteractionCollection {
        val actual = resolveAll().size
        if (actual != expected) {
            throw AssertionError(
                "Expected $expected nodes matching '${matcher.description}' but found $actual.\n" +
                    "Tree:\n${test.root.dumpTree()}",
            )
        }
        return this
    }

    /** Returns the number of currently matching nodes. */
    public fun fetchSize(): Int = resolveAll().size

    /**
     * Resolves every matching component and returns them typed as [T], in depth-first pre-order, for
     * reading or driving each component's own API (e.g. a `JList`'s model, a `JSplitPane`'s divider).
     *
     * @throws AssertionError if any matched node is not a [T].
     */
    public inline fun <reified T : Component> fetchAll(): List<T> =
        resolveAll().map { component ->
            component as? T
                ?: throw AssertionError(
                    "Node is a ${component.javaClass.simpleName}, " +
                        "expected a ${T::class.java.simpleName}.",
                )
        }
}
