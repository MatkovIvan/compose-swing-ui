package org.jetbrains.compose.swing.test.interaction

import org.jetbrains.compose.swing.test.SwingMatcher
import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.dumpTree
import org.jetbrains.compose.swing.test.findMatching
import java.awt.Component
import java.awt.Container
import javax.swing.SwingUtilities

/**
 * A lazy handle to the set of components matched by a query. The match set is resolved against the
 * live AWT tree each time it is needed, so it reflects the current tree.
 *
 * All methods are intended to be called from a [org.jetbrains.compose.swing.test.runSwingUiTest]
 * body, which runs on the EDT.
 */
public class SwingNodeInteractionCollection internal constructor(
    private val test: SwingUiTest,
    private val matcher: SwingMatcher,
    private val root: () -> Container,
    private val ancestor: Component? = null,
) {
    @PublishedApi
    internal fun resolveAll(): List<Component> {
        val matches = root().findMatching(matcher)
        val scope = ancestor ?: return matches
        return matches.filter { it !== scope && SwingUtilities.isDescendingFrom(it, scope) }
    }

    /** Human-readable description of this collection's query, for [fetchAll] failure messages. */
    @PublishedApi
    internal val matcherDescription: String
        get() = matcher.description

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
        SwingNodeInteractionCollection(test, matcher, root, ancestor)

    /**
     * Returns a lazy handle to the match at [index], in depth-first pre-order. Like every
     * interaction, the handle re-resolves against the live tree on each use, so it tracks matches
     * added or removed by recomposition; it fails on use when fewer than `index + 1` nodes match.
     *
     * ```
     * onAllNodesWithText("row")[1].assertIsEnabled()
     * ```
     */
    public operator fun get(index: Int): SwingNodeInteraction =
        SwingNodeInteraction(test, "${matcher.description}[$index]", root, NodePick.AtIndex(index), ::resolveAll)

    /**
     * Returns a lazy handle to the first match, in depth-first pre-order. Convenience for
     * [get]`(0)`.
     */
    public fun onFirst(): SwingNodeInteraction = get(0)

    /**
     * Returns a lazy handle to the last match, in depth-first pre-order. The handle re-resolves
     * against the live tree on each use, so it tracks the current last match across recomposition;
     * it fails on use when nothing matches.
     */
    public fun onLast(): SwingNodeInteraction =
        SwingNodeInteraction(test, "${matcher.description}.onLast()", root, NodePick.Last, ::resolveAll)

    /** Asserts that exactly [expected] nodes match. */
    public fun assertCountEquals(expected: Int): SwingNodeInteractionCollection {
        val actual = resolveAll().size
        if (actual != expected) {
            throw AssertionError(
                "Expected $expected nodes matching '${matcher.description}' but found $actual.\n" +
                    "Tree:\n${root().dumpTree()}",
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
        resolveAll().map { component -> component.castOrFail<T>("Node", matcherDescription) }
}
