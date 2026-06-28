package org.jetbrains.compose.swing.test.interaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.layout.BoxPanel
import org.jetbrains.compose.swing.components.layout.Panel
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.name
import org.jetbrains.compose.swing.test.SwingMatcher
import org.jetbrains.compose.swing.test.onAllNodesOfType
import org.jetbrains.compose.swing.test.runSwingUiTest
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * Behavioral coverage for [SwingNodeInteractionCollection], the multi-match handle returned by
 * [SwingUiTest.onAllNodesWithText]. The collection is lazy: it re-resolves its match set against the
 * live AWT tree each time it is queried, so the same handle reflects components added or removed by
 * recomposition. These tests pin that re-resolution, the zero-match case, the chaining contract of
 * [SwingNodeInteractionCollection.assertCountEquals], and that a wrong count assertion fails.
 */
class NodeCollectionInteractionTest {
    @Test
    fun countAndFetchSizeAgreeForACollection() = runSwingUiTest {
        setContent {
            BoxPanel {
                Label(text = "row")
                Label(text = "row")
                Label(text = "row")
                Label(text = "other")
            }
        }
        onAllNodesWithText("row").assertCountEquals(3)
        assertEquals(3, onAllNodesWithText("row").fetchSize(), "fetchSize should agree with the three \"row\" labels")
        assertEquals(1, onAllNodesWithText("other").fetchSize(), "the single \"other\" label should match once")
    }

    @Test
    fun fetchSizeIsZeroWhenNothingMatches() = runSwingUiTest {
        setContent { Label(text = "present") }
        assertEquals(0, onAllNodesWithText("absent").fetchSize())
        onAllNodesWithText("absent").assertCountEquals(0)
    }

    @Test
    fun assertCountEqualsReturnsTheSameCollectionForChaining() = runSwingUiTest {
        setContent {
            BoxPanel {
                Label(text = "row")
                Label(text = "row")
            }
        }
        val collection = onAllNodesWithText("row")
        // assertCountEquals returns the same handle so further queries can be chained on it.
        assertSame(collection, collection.assertCountEquals(2))
        collection.assertCountEquals(2).assertCountEquals(2)
    }

    @Test
    fun aWrongCountAssertionFails() = runSwingUiTest {
        setContent {
            BoxPanel {
                Label(text = "row")
                Label(text = "row")
            }
        }
        assertFailsWith<AssertionError> { onAllNodesWithText("row").assertCountEquals(3) }
    }

    @Test
    fun fetchAllReturnsEveryMatchingComponentTypedAndInTreeOrder() = runSwingUiTest {
        setContent {
            BoxPanel {
                Label(text = "alpha")
                Label(text = "beta")
                Button(text = "go")
            }
        }
        val labels = onAllNodesOfType<JLabel>().fetchAll<JLabel>()
        assertEquals(listOf("alpha", "beta"), labels.map { it.text })
    }

    @Test
    fun fetchAllFailsWhenAMatchedNodeIsNotTheRequestedType() = runSwingUiTest {
        setContent {
            BoxPanel {
                Label(text = "alpha")
                Button(text = "go")
            }
        }
        // Every component matches the all-matching base query, but they are not all JLabels.
        assertFailsWith<AssertionError> { onAllNodes(SwingMatcher.isEnabled()).fetchAll<JLabel>() }
    }

    @Test
    fun withinNarrowsAQueryToTheGivenSubtree() = runSwingUiTest {
        setContent {
            BoxPanel {
                Panel(modifier = SwingModifier.name("inside")) {
                    Label(text = "alpha")
                    Label(text = "beta")
                }
                Label(text = "outside")
            }
        }
        // The tree-wide query sees all three labels; scoping to the named subtree keeps only the
        // two that descend from it.
        assertEquals(3, onAllNodesOfType<JLabel>().fetchSize(), "the tree-wide query should see all three labels")

        val inside = onNodeWithName("inside").fetch<JPanel>()
        val scoped = onAllNodesOfType<JLabel>().within(inside).fetchAll<JLabel>()
        assertEquals(
            listOf("alpha", "beta"),
            scoped.map { it.text },
            "within should keep only the subtree's two labels",
        )
    }

    @Test
    fun aHeldCollectionReResolvesAsRecompositionAddsAndRemovesMatches() = runSwingUiTest {
        var rows by mutableIntStateOf(1)
        setContent {
            BoxPanel {
                repeat(rows) { Label(text = "row") }
            }
        }
        // The same handle is captured once and re-queried after each state change; it tracks the
        // live tree rather than a snapshot taken when it was created.
        val collection = onAllNodesWithText("row")
        assertEquals(1, collection.fetchSize(), "the held collection should start with the single row")

        rows = 4
        awaitIdle()
        assertEquals(4, collection.fetchSize(), "the held collection should re-resolve to four rows after the add")
        collection.assertCountEquals(4)

        rows = 0
        awaitIdle()
        assertEquals(0, collection.fetchSize(), "the held collection should re-resolve to zero rows after the removal")
    }
}
