package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.layout.ScrollPane
import org.jetbrains.compose.swing.components.selection.Tree
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import java.awt.Dimension

/** A node in the sample data tree: a [name] and its ordered [children]. */
private class Node(
    val name: String,
    val children: List<Node> = emptyList(),
)

private val sampleTree =
    Node(
        "Project",
        listOf(
            Node(
                "src",
                listOf(
                    Node("main", listOf(Node("App.kt"), Node("Tree.kt"))),
                    Node("test", listOf(Node("TreeTest.kt"))),
                ),
            ),
            Node(
                "docs",
                listOf(Node("README.md"), Node("ARCHITECTURE.md")),
            ),
            Node("build.gradle.kts"),
        ),
    )

/**
 * Demonstrates [Tree]: a nested data structure rendered through the data-driven API ([root] +
 * [children] + [label]), wrapped in a [ScrollPane], with the selection driven by state and echoed
 * into a [Label].
 */
@Composable
internal fun TreeSection() {
    SectionColumn {
        SectionHeading("Tree")
        ExampleCard("Tree from nested data with selection bound to state") {
            var selection by remember { mutableStateOf<List<List<Int>>>(emptyList()) }

            ScrollPane(modifier = SwingModifier.preferredSize(Dimension(360, 220))) {
                content {
                    Tree(
                        root = sampleTree,
                        children = { it.children },
                        label = { it.name },
                        selectedPaths = selection,
                        onSelectionChange = { selection = it },
                    )
                }
            }
            Label(text = "Selected path: ${describeSelection(selection)}")
        }
    }
}

/** Resolves the first selected index path back to readable node names for display. */
private fun describeSelection(selection: List<List<Int>>): String {
    val path = selection.firstOrNull() ?: return "(none)"
    val names = ArrayList<String>(path.size + 1)
    var node = sampleTree
    names.add(node.name)
    for (index in path) {
        node = node.children[index]
        names.add(node.name)
    }
    return names.joinToString(" / ")
}
