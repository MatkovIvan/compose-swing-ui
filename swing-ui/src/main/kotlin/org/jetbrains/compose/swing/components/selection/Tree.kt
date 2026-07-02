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
import javax.swing.tree.TreeModel
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
    Tree(
        root = root,
        children = children,
        treeSelectionListener = rememberSelectionListener(onSelectionChange),
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
            set(selectedPaths) { applySelection(this, this.model, it) }
            applyModifier(SwingModifier.treeSelectionListener(treeSelectionListener) then modifier)
        },
    )
}

/**
 * A composable wrapper for `JTree` driven by a caller-owned [TreeModel].
 *
 * The [model] is displayed as-is: its own nodes and structure drive the tree, and the library never
 * mutates it. Supplying a new [model] instance swaps it into the tree on recomposition. Selection is
 * controlled via [selectedPaths] + [onSelectionChange], each path expressed as the chain of child
 * indices from the root (so `[]` is the root, `[0]` its first child, `[0, 2]` that child's third
 * child); the indices are resolved through the model's own accessors, so any [TreeModel] works. A
 * model swap clears the tree's selection, after which [selectedPaths] is re-applied so the controlled
 * selection survives. Place it in a [ScrollPane] to scroll.
 *
 * ```
 * ScrollPane {
 *     content {
 *         Tree(
 *             model = fileSystemModel,
 *             selectedPaths = selection,
 *             onSelectionChange = { selection = it },
 *         )
 *     }
 * }
 * ```
 *
 * @param model the tree model to display; owned by the caller and never mutated by the library
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedPaths the currently selected nodes as index paths from the root (controlled);
 *   re-applied after a model swap
 * @param onSelectionChange callback invoked when the selection changes
 * @param selectionMode how many nodes may be selected
 * @param rootVisible whether the root node is shown
 * @param showsRootHandles whether expand/collapse handles are shown for the top-level nodes
 */
@Composable
public fun Tree(
    model: TreeModel,
    modifier: SwingModifier = SwingModifier,
    selectedPaths: List<List<Int>> = emptyList(),
    onSelectionChange: (List<List<Int>>) -> Unit = {},
    @TreeSelectionMode selectionMode: Int = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION,
    rootVisible: Boolean = true,
    showsRootHandles: Boolean = false,
) {
    Tree(
        model = model,
        treeSelectionListener = rememberSelectionListener(onSelectionChange),
        modifier = modifier,
        selectedPaths = selectedPaths,
        selectionMode = selectionMode,
        rootVisible = rootVisible,
        showsRootHandles = showsRootHandles,
    )
}

/**
 * A model-driven [Tree] driven by a raw [TreeSelectionListener] instead of an `onSelectionChange`
 * lambda. The listener is attached as-is and removed on the same instance; pass a stable instance
 * (e.g. `remember {}`) to avoid churn.
 *
 * The [model] is displayed as-is and never mutated by the library. A model swap clears the tree's
 * selection, after which [selectedPaths] is re-applied so the controlled selection survives.
 *
 * @param model the tree model to display; owned by the caller and never mutated by the library
 * @param treeSelectionListener the listener notified of selection changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedPaths the currently selected nodes as index paths from the root (controlled);
 *   re-applied after a model swap
 * @param selectionMode how many nodes may be selected
 * @param rootVisible whether the root node is shown
 * @param showsRootHandles whether expand/collapse handles are shown for the top-level nodes
 */
@Composable
public fun Tree(
    model: TreeModel,
    treeSelectionListener: TreeSelectionListener,
    modifier: SwingModifier = SwingModifier,
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
            // setModel clears the selection, so re-apply the controlled selection to keep it intact
            // across a model swap.
            set(model) {
                this.model = it
                applySelection(this, it, selectedPaths)
            }
            set(selectedPaths) { applySelection(this, this.model, it) }
            applyModifier(SwingModifier.treeSelectionListener(treeSelectionListener) then modifier)
        },
    )
}

/**
 * Remembers a [TreeSelectionListener] that reports the tree's selection back through [onSelectionChange]
 * as index paths. A tree selection event's source is the `JTree` itself, so the selection is read back
 * from its model; [onSelectionChange] is tracked with [rememberUpdatedState] so the remembered listener
 * always calls the latest callback without being recreated.
 */
@Composable
private fun rememberSelectionListener(onSelectionChange: (List<List<Int>>) -> Unit): TreeSelectionListener {
    val callback = rememberUpdatedState(onSelectionChange)
    return remember {
        TreeSelectionListener { event ->
            val tree = event.source as JTree
            callback.value(readSelection(tree, tree.model))
        }
    }
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
 *
 * The walk goes through the model's own accessors (`getChildCount`/`getChild`), so any [TreeModel]
 * resolves regardless of the node type it exposes.
 */
private fun resolvePath(
    model: TreeModel,
    indices: List<Int>,
): TreePath? {
    var node: Any? = model.root
    val nodes = ArrayList<Any>(indices.size + 1)
    for (index in indices) {
        if (node == null || index !in 0 until model.getChildCount(node)) return null
        nodes.add(node)
        node = model.getChild(node, index)
    }
    return node?.let { TreePath((nodes + it).toTypedArray()) }
}

/**
 * Converts a [TreePath] of nodes back to its chain of child indices from the root (the root itself
 * contributes no index, so the root path maps to the empty list).
 *
 * Each index is the child's position under its parent as [model] reports it (`getIndexOfChild`), so
 * the mapping holds for any [TreeModel] independent of its node type.
 */
private fun pathToIndices(
    model: TreeModel,
    path: TreePath,
): List<Int> {
    val nodes = path.path
    return (1 until nodes.size).map { i -> model.getIndexOfChild(nodes[i - 1], nodes[i]) }
}

/**
 * Reads the tree's current selection back as index paths (each the chain of child indices from the
 * root to a selected node), in the order the selection model reports them.
 */
private fun readSelection(
    tree: JTree,
    model: TreeModel,
): List<List<Int>> = tree.selectionPaths?.map { pathToIndices(model, it) }.orEmpty()

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
    model: TreeModel,
    selectedPaths: List<List<Int>>,
) {
    val resolved = selectedPaths.mapNotNull { resolvePath(model, it) }
    if (readSelection(tree, model) == resolved.map { pathToIndices(model, it) }) return
    if (resolved.isEmpty()) {
        tree.clearSelection()
    } else {
        tree.selectionPaths = resolved.toTypedArray()
    }
}
