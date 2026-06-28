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
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionListener

/**
 * A composable wrapper for `JList`.
 *
 * Items are declarative data, rendered through the list's cell renderer (each item's `toString` by
 * default, or a custom [cellRenderer]). Selection is controlled via [selectedIndices] +
 * [onSelectionChange], expressed as the general multi-select shape so one component covers all of
 * [SelectionMode]'s modes. Place it in a [ScrollPane] to scroll:
 *
 * ```
 * ScrollPane {
 *     content {
 *         ListBox(items = rows, selectedIndices = sel, onSelectionChange = { sel = it })
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
 * @param cellRenderer optional custom renderer; `null` keeps the default
 */
@Composable
public fun <T> ListBox(
    items: List<T>,
    modifier: SwingModifier = SwingModifier,
    selectedIndices: List<Int> = emptyList(),
    onSelectionChange: (List<Int>) -> Unit = {},
    @SelectionMode selectionMode: Int = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
    visibleRowCount: Int = 8,
    cellRenderer: ListCellRenderer<in T>? = null,
) {
    val callback = rememberUpdatedState(onSelectionChange)
    // A JList re-fires its selection event with the list itself as the source, so read the settled
    // selection back from the list once the value stops adjusting.
    val listener =
        remember {
            ListSelectionListener { event ->
                if (!event.valueIsAdjusting) callback.value((event.source as JList<*>).selectedIndices.toList())
            }
        }
    ListBox(
        items = items,
        listSelectionListener = listener,
        modifier = modifier,
        selectedIndices = selectedIndices,
        selectionMode = selectionMode,
        visibleRowCount = visibleRowCount,
        cellRenderer = cellRenderer,
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
 * @param cellRenderer optional custom renderer; `null` keeps the default
 */
@Composable
public fun <T> ListBox(
    items: List<T>,
    listSelectionListener: ListSelectionListener,
    modifier: SwingModifier = SwingModifier,
    selectedIndices: List<Int> = emptyList(),
    @SelectionMode selectionMode: Int = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
    visibleRowCount: Int = 8,
    cellRenderer: ListCellRenderer<in T>? = null,
) {
    SwingNode(
        factory = { JList<T>() },
        update = {
            set(selectionMode) { this.selectionMode = it }
            set(visibleRowCount) { this.visibleRowCount = it }
            set(cellRenderer) { renderer -> if (renderer != null) this.cellRenderer = renderer }
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
