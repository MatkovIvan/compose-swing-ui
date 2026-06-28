@file:JvmMultifileClass
@file:JvmName("SelectionComponentsKt")

package org.jetbrains.compose.swing.components.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.constants.TreeSelectionMode
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.treeSelectionListener
import javax.swing.JTree
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

/**
 * A composable wrapper for `JTree`.
 *
 * The tree is described as data: [root] is the root value and [children] yields each value's child
 * values, walked recursively to build the displayed structure; [label] renders each value's row text
 * (its `toString` by default). The structure reflects the last composition — changing the data the
 * accessors return rebuilds the tree on recompose. Selection is controlled via [selectedPaths] +
 * [onSelectionChange], each path expressed as the chain of child indices from the root (so `[]` is the
 * root, `[0]` its first child, `[0, 2]` that child's third child). Place it in a [ScrollPane] to
 * scroll.
 *
 * ```
 * ScrollPane {
 *     content {
 *         Tree(
 *             root = fileSystem,
 *             children = { it.entries },
 *             label = { it.name },
 *             selectedPaths = selection,
 *             onSelectionChange = { selection = it },
 *         )
 *     }
 * }
 * ```
 *
 * @param root the root value of the tree
 * @param children yields the child values of a value, in display order
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param label renders a value's row text
 * @param selectedPaths the currently selected nodes as index paths from the root (controlled)
 * @param onSelectionChange callback invoked when the selection changes
 * @param selectionMode how many nodes may be selected
 * @param rootVisible whether the root node is shown
 * @param showsRootHandles whether expand/collapse handles are shown for the top-level nodes
 */
@Composable
public fun <T> Tree(
    root: T,
    children: (T) -> List<T>,
    modifier: SwingModifier = SwingModifier,
    label: (T) -> String = { it.toString() },
    selectedPaths: List<List<Int>> = emptyList(),
    onSelectionChange: (List<List<Int>>) -> Unit = {},
    @TreeSelectionMode selectionMode: Int = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION,
    rootVisible: Boolean = true,
    showsRootHandles: Boolean = false,
) {
    val callback = rememberUpdatedState(onSelectionChange)
    // A tree selection event's source is the JTree itself, so read the selection back from it.
    val listener = remember { TreeSelectionListener { event -> callback.value(readSelection(event.source as JTree)) } }
    Tree(
        root = root,
        children = children,
        treeSelectionListener = listener,
        modifier = modifier,
        label = label,
        selectedPaths = selectedPaths,
        selectionMode = selectionMode,
        rootVisible = rootVisible,
        showsRootHandles = showsRootHandles,
    )
}

/**
 * A [Tree] driven by a raw [TreeSelectionListener] instead of an `onSelectionChange` lambda. The
 * listener is attached as-is and removed on the same instance; pass a stable instance (e.g.
 * `remember {}`) to avoid churn.
 *
 * @param root the root value of the tree
 * @param children yields the child values of a value, in display order
 * @param treeSelectionListener the listener notified of selection changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param label renders a value's row text
 * @param selectedPaths the currently selected nodes as index paths from the root (controlled)
 * @param selectionMode how many nodes may be selected
 * @param rootVisible whether the root node is shown
 * @param showsRootHandles whether expand/collapse handles are shown for the top-level nodes
 */
@Composable
public fun <T> Tree(
    root: T,
    children: (T) -> List<T>,
    treeSelectionListener: TreeSelectionListener,
    modifier: SwingModifier = SwingModifier,
    label: (T) -> String = { it.toString() },
    selectedPaths: List<List<Int>> = emptyList(),
    @TreeSelectionMode selectionMode: Int = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION,
    rootVisible: Boolean = true,
    showsRootHandles: Boolean = false,
) {
    SwingNode(
        factory = { JTree(DefaultTreeModel(DefaultMutableTreeNode())) },
        update = {
            set(selectionMode) { this.selectionModel.selectionMode = it }
            set(rootVisible) { this.isRootVisible = it }
            set(showsRootHandles) { this.setShowsRootHandles(it) }
            set(Triple(root, children, label)) { (rootValue, childrenOf, labelOf) ->
                val model = DefaultTreeModel(buildNode(rootValue, childrenOf, labelOf))
                this.model = model
                applySelection(this, model, selectedPaths)
            }
            set(selectedPaths) { applySelection(this, this.model as DefaultTreeModel, it) }
            applyModifier(SwingModifier.treeSelectionListener(treeSelectionListener) then modifier)
        },
    )
}

/**
 * Builds a [DefaultMutableTreeNode] tree from [root] by recursively visiting [children]. Each node's
 * user object is [label] applied to the value (a `String`), so the default `JTree` renderer shows the
 * label with no per-node cast. The returned node mirrors the data tree one-to-one in structure and
 * child order.
 */
private fun <T> buildNode(
    value: T,
    children: (T) -> List<T>,
    label: (T) -> String,
): DefaultMutableTreeNode {
    val node = DefaultMutableTreeNode(label(value))
    for (child in children(value)) {
        node.add(buildNode(child, children, label))
    }
    return node
}

/**
 * Resolves a path of child indices (from the root down) to a [TreePath] of nodes in [model], or `null`
 * if any index is out of range for the current structure. Index `[]` is the root; `[i]` is the root's
 * i-th child; and so on.
 */
private fun resolvePath(
    model: DefaultTreeModel,
    indices: List<Int>,
): TreePath? {
    var node = model.root as DefaultMutableTreeNode
    val nodes = ArrayList<DefaultMutableTreeNode>(indices.size + 1)
    nodes.add(node)
    for (index in indices) {
        if (index !in 0 until node.childCount) return null
        node = node.getChildAt(index) as DefaultMutableTreeNode
        nodes.add(node)
    }
    return TreePath(nodes.toTypedArray())
}

/**
 * Converts a [TreePath] of nodes back to its chain of child indices from the root (the root itself
 * contributes no index, so the root path maps to the empty list).
 */
private fun pathToIndices(path: TreePath): List<Int> =
    path.path.drop(1).map { node ->
        val child = node as DefaultMutableTreeNode
        (child.parent as DefaultMutableTreeNode).getIndex(child)
    }

/**
 * Reads the tree's current selection back as index paths (each the chain of child indices from the
 * root to a selected node), in the order the selection model reports them.
 */
private fun readSelection(tree: JTree): List<List<Int>> = tree.selectionPaths?.map(::pathToIndices).orEmpty()

/**
 * Re-applies [selectedPaths] as the tree's selection, but only when it differs from the current
 * selection. Paths that no longer resolve against the current structure are dropped.
 *
 * Replacing the model clears the selection, so the caller re-applies after every structure change; and
 * a guard against re-setting an unchanged selection keeps a programmatic set from echoing back through
 * the selection listener as a spurious `onSelectionChange`.
 */
private fun applySelection(
    tree: JTree,
    model: DefaultTreeModel,
    selectedPaths: List<List<Int>>,
) {
    val resolved = selectedPaths.mapNotNull { resolvePath(model, it) }
    if (readSelection(tree) == resolved.map(::pathToIndices)) return
    if (resolved.isEmpty()) {
        tree.clearSelection()
    } else {
        tree.selectionPaths = resolved.toTypedArray()
    }
}
