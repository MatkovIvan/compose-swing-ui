@file:JvmMultifileClass
@file:JvmName("SelectionComponentsKt")

package org.jetbrains.compose.swing.components.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.constants.SelectionMode
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.listSelectionListener
import java.util.Vector
import javax.swing.JList
import javax.swing.ListModel
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionListener

/**
 * A composable wrapper for `JList`.
 *
 * Items are declarative data. By default each row renders its item's `toString`; supply [itemContent]
 * to render an arbitrary composable cell per row (a `Row` of an icon, labels, …). Selection is
 * controlled via [selectedIndices] + [onSelectionChange], expressed as the general multi-select shape
 * so one component covers all of [SelectionMode]'s modes. Place it in a [ScrollPane] to scroll:
 *
 * ```
 * ScrollPane {
 *     content {
 *         ListBox(items = rows, selectedIndices = sel, onSelectionChange = { sel = it }) { row ->
 *             FlowPanel { Label(row.icon); Label(row.name) }
 *         }
 *     }
 * }
 * ```
 *
 * [onSelectionChange] fires once per settled selection change, so dragging across rows produces one
 * callback at the end rather than one per row crossed.
 *
 * @param items the items to display
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedIndices the currently selected row indices (controlled)
 * @param onSelectionChange callback invoked when the settled selection changes
 * @param selectionMode how many rows/ranges may be selected
 * @param visibleRowCount preferred number of visible rows (`JList.setVisibleRowCount`)
 * @param itemContent optional composable cell rendered per row against a [ListItemScope]; `null` keeps
 *   the default `toString` rendering
 */
@Composable
public fun <T> ListBox(
    items: List<T>,
    modifier: SwingModifier = SwingModifier,
    selectedIndices: List<Int> = emptyList(),
    onSelectionChange: (List<Int>) -> Unit = {},
    @SelectionMode selectionMode: Int = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
    visibleRowCount: Int = 8,
    itemContent: (@Composable ListItemScope.(item: T) -> Unit)? = null,
) {
    ListBox(
        items = items,
        listSelectionListener = rememberSettledSelectionListener(onSelectionChange),
        modifier = modifier,
        selectedIndices = selectedIndices,
        selectionMode = selectionMode,
        visibleRowCount = visibleRowCount,
        itemContent = itemContent,
    )
}

/**
 * A [ListBox] driven by a raw [ListSelectionListener] instead of an `onSelectionChange` lambda. The
 * listener is attached as-is and removed on the same instance; pass a stable instance (e.g.
 * `remember {}`) to avoid churn.
 *
 * @param items the items to display
 * @param listSelectionListener the listener notified of selection changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedIndices the currently selected row indices (controlled)
 * @param selectionMode how many rows/ranges may be selected
 * @param visibleRowCount preferred number of visible rows (`JList.setVisibleRowCount`)
 * @param itemContent optional composable cell rendered per row against a [ListItemScope]; `null` keeps
 *   the default `toString` rendering
 */
@Composable
public fun <T> ListBox(
    items: List<T>,
    listSelectionListener: ListSelectionListener,
    modifier: SwingModifier = SwingModifier,
    selectedIndices: List<Int> = emptyList(),
    @SelectionMode selectionMode: Int = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
    visibleRowCount: Int = 8,
    itemContent: (@Composable ListItemScope.(item: T) -> Unit)? = null,
) {
    // The single conversion from itemContent to a JList cell renderer: one reused ComposingListCellRenderer
    // stamps a recycled composition per row. A null itemContent keeps the JList default toString renderer.
    val renderer = itemContent?.let { rememberComposingListCellRenderer(it) }
    SwingNode(
        factory = { JList<T>() },
        update = {
            set(selectionMode) { this.selectionMode = it }
            set(visibleRowCount) { this.visibleRowCount = it }
            set(renderer) { it?.let { r -> this.cellRenderer = r } }
            set(items) {
                this.setListData(Vector(it))
                applySelection(this, selectedIndices)
            }
            set(selectedIndices) { applySelection(this, it) }
            applyModifier(SwingModifier.listSelectionListener(listSelectionListener) then modifier)
        },
    )
}

/**
 * A [ListBox] driven by a caller-owned [ListModel] instead of a declarative `items` list. The model
 * is installed as-is and observed only: the library never mutates it, so element changes are the
 * caller's responsibility. Selection stays controlled via [selectedIndices] + [onSelectionChange].
 *
 * Swapping in a new model instance clears the list's selection, so the wrapper re-applies
 * [selectedIndices] after the swap; the controlled selection therefore survives a model change.
 *
 * ```
 * ScrollPane {
 *     content {
 *         ListBox(model = myModel, selectedIndices = sel, onSelectionChange = { sel = it })
 *     }
 * }
 * ```
 *
 * [onSelectionChange] fires once per settled selection change, so dragging across rows produces one
 * callback at the end rather than one per row crossed.
 *
 * @param model the caller-owned list model to display; installed as-is and never mutated
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedIndices the currently selected row indices (controlled)
 * @param onSelectionChange callback invoked when the settled selection changes
 * @param selectionMode how many rows/ranges may be selected
 * @param visibleRowCount preferred number of visible rows (`JList.setVisibleRowCount`)
 * @param itemContent optional composable cell rendered per row against a [ListItemScope]; `null` keeps
 *   the default `toString` rendering
 */
@Composable
public fun <T> ListBox(
    model: ListModel<T>,
    modifier: SwingModifier = SwingModifier,
    selectedIndices: List<Int> = emptyList(),
    onSelectionChange: (List<Int>) -> Unit = {},
    @SelectionMode selectionMode: Int = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
    visibleRowCount: Int = 8,
    itemContent: (@Composable ListItemScope.(item: T) -> Unit)? = null,
) {
    ListBox(
        model = model,
        listSelectionListener = rememberSettledSelectionListener(onSelectionChange),
        modifier = modifier,
        selectedIndices = selectedIndices,
        selectionMode = selectionMode,
        visibleRowCount = visibleRowCount,
        itemContent = itemContent,
    )
}

/**
 * A model-driven [ListBox] driven by a raw [ListSelectionListener] instead of an `onSelectionChange`
 * lambda. The listener is attached as-is and removed on the same instance; pass a stable instance
 * (e.g. `remember {}`) to avoid churn.
 *
 * The [model] is installed as-is and observed only: the library never mutates it. Swapping in a new
 * model instance clears the list's selection, so the wrapper re-applies [selectedIndices] after the
 * swap; the controlled selection therefore survives a model change.
 *
 * @param model the caller-owned list model to display; installed as-is and never mutated
 * @param listSelectionListener the listener notified of selection changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedIndices the currently selected row indices (controlled)
 * @param selectionMode how many rows/ranges may be selected
 * @param visibleRowCount preferred number of visible rows (`JList.setVisibleRowCount`)
 * @param itemContent optional composable cell rendered per row against a [ListItemScope]; `null` keeps
 *   the default `toString` rendering
 */
@Composable
public fun <T> ListBox(
    model: ListModel<T>,
    listSelectionListener: ListSelectionListener,
    modifier: SwingModifier = SwingModifier,
    selectedIndices: List<Int> = emptyList(),
    @SelectionMode selectionMode: Int = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
    visibleRowCount: Int = 8,
    itemContent: (@Composable ListItemScope.(item: T) -> Unit)? = null,
) {
    // The single conversion from itemContent to a JList cell renderer: one reused ComposingListCellRenderer
    // stamps a recycled composition per row. A null itemContent keeps the JList default toString renderer.
    val renderer = itemContent?.let { rememberComposingListCellRenderer(it) }
    SwingNode(
        factory = { JList<T>() },
        update = {
            set(selectionMode) { this.selectionMode = it }
            set(visibleRowCount) { this.visibleRowCount = it }
            set(renderer) { it?.let { r -> this.cellRenderer = r } }
            set(model) {
                this.model = it
                applySelection(this, selectedIndices)
            }
            set(selectedIndices) { applySelection(this, it) }
            applyModifier(SwingModifier.listSelectionListener(listSelectionListener) then modifier)
        },
    )
}

/**
 * Adapts an `onSelectionChange` lambda into the raw [ListSelectionListener] the model-agnostic
 * overloads delegate to. The lambda is captured through [rememberUpdatedState] so a recomposition
 * with a new lambda is honoured without rebuilding the listener.
 */
@Composable
private fun rememberSettledSelectionListener(onSelectionChange: (List<Int>) -> Unit): ListSelectionListener {
    val callback = rememberUpdatedState(onSelectionChange)
    // A JList re-fires its selection event with the list itself as the source, so read the settled
    // selection back from the list once the value stops adjusting.
    return remember {
        ListSelectionListener { event ->
            if (!event.valueIsAdjusting) callback.value((event.source as JList<*>).selectedIndices.toList())
        }
    }
}

/**
 * Re-applies [indices] as the list's selection, but only when it differs from the current selection.
 *
 * `setListData` clears the selection, so the caller re-applies after every items change; and a guard
 * against re-setting an unchanged selection keeps a programmatic set from echoing back through the
 * selection listener as a spurious `onSelectionChange`.
 */
private fun applySelection(
    list: JList<*>,
    indices: List<Int>,
) {
    val itemCount = list.model.size
    val valid = indices.filter { it in 0 until itemCount }
    if (list.selectedIndices.toList() == valid) return
    list.selectedIndices = valid.toIntArray()
}
