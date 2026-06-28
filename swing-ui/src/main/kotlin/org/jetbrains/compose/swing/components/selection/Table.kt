@file:JvmMultifileClass
@file:JvmName("SelectionComponentsKt")

package org.jetbrains.compose.swing.components.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.components.selectedIndices
import org.jetbrains.compose.swing.components.tableSelectionListener
import org.jetbrains.compose.swing.constants.SelectionMode
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionListener
import javax.swing.table.AbstractTableModel

/**
 * A composable wrapper for `JTable` with declarative, data-driven rows and columns.
 *
 * [rows] are the row data; declare each column in [block] with its header, a value extractor, and
 * (optionally) in-place editing. Rows and columns are **data**: changing [rows] or the declared
 * columns rebuilds the table's model on recomposition. Selection is controlled via
 * [selectedRowIndices] + [onSelectionChange], expressed as the general multi-select shape so one
 * component covers all of [SelectionMode]'s modes. Place it in a [ScrollPane] to scroll and to show
 * the column header.
 *
 * ```
 * ScrollPane {
 *     content {
 *         Table(
 *             rows = people,
 *             selectedRowIndices = selection,
 *             onSelectionChange = { selection = it },
 *         ) {
 *             column("Name") { it.name }
 *             column("Age", isEditable = true, onCellEdit = { row, _, v -> update(row, v) }) { it.age }
 *         }
 *     }
 * }
 * ```
 *
 * A cell edit commits through the edited column's `onCellEdit`; the displayed value does not change
 * until the caller updates the backing state and the next composition supplies fresh [rows].
 * [onSelectionChange] fires once per settled selection change, so dragging across rows produces one
 * callback at the end rather than one per row crossed.
 *
 * @param rows the row data to display
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedRowIndices the currently selected row indices (controlled)
 * @param onSelectionChange callback invoked when the settled row selection changes
 * @param selectionMode how many rows/ranges may be selected
 * @param block declares the columns; see [TableScope]
 */
@Composable
public fun <R> Table(
    rows: List<R>,
    modifier: SwingModifier = SwingModifier,
    selectedRowIndices: List<Int> = emptyList(),
    onSelectionChange: (List<Int>) -> Unit = {},
    @SelectionMode selectionMode: Int = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
    block: TableScope<R>.() -> Unit,
) {
    val callback = rememberUpdatedState(onSelectionChange)
    // The table registers the listener on its selection model, so the event source is the model; read
    // the settled row selection from it once the value stops adjusting.
    val listener =
        remember {
            ListSelectionListener { event ->
                if (!event.valueIsAdjusting) callback.value((event.source as ListSelectionModel).selectedIndices())
            }
        }
    Table(
        rows = rows,
        listSelectionListener = listener,
        modifier = modifier,
        selectedRowIndices = selectedRowIndices,
        selectionMode = selectionMode,
        block = block,
    )
}

/**
 * A [Table] driven by a raw [ListSelectionListener] instead of an `onSelectionChange` lambda. The
 * listener is attached to the table's `selectionModel` as-is (so it observes the same settled-or-raw
 * selection events the model publishes) and removed on the same instance; pass a stable instance (e.g.
 * `remember {}`) to avoid churn.
 *
 * @param rows the row data to display
 * @param listSelectionListener the listener notified of selection-model changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedRowIndices the currently selected row indices (controlled)
 * @param selectionMode how many rows/ranges may be selected
 * @param block declares the columns; see [TableScope]
 */
@Composable
public fun <R> Table(
    rows: List<R>,
    listSelectionListener: ListSelectionListener,
    modifier: SwingModifier = SwingModifier,
    selectedRowIndices: List<Int> = emptyList(),
    @SelectionMode selectionMode: Int = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
    block: TableScope<R>.() -> Unit,
) {
    val columns = TableScopeImpl<R>().apply(block).columns
    val model = remember { TableModel<R>() }

    SwingNode(
        factory = { JTable(model) },
        update = {
            set(selectionMode) { this.setSelectionMode(it) }
            reconcile {
                val structureChanged = model.refresh(rows, columns)
                if (structureChanged) applySelection(this, selectedRowIndices)
            }
            set(selectedRowIndices) { applySelection(this, it) }
            applyModifier(
                SwingModifier.tableSelectionListener(listSelectionListener) then modifier,
            )
        },
    )
}

/**
 * Declares the columns of a [Table]. Each [column] call appends one column, in call order.
 *
 * The receiver type parameter [R] is the table's row type; a column's [column] `value` extractor and
 * `onCellEdit` callback are expressed in terms of [R].
 */
public interface TableScope<R> {
    /**
     * Declares one column.
     *
     * @param header the column's header text
     * @param value extracts the cell value to display for a given row
     * @param isEditable whether this column's cells can be edited in place; `false` (the default)
     *   makes the whole column read-only
     * @param onCellEdit invoked when a cell in this column is edited and the edit is committed,
     *   receiving the row, the row index, and the newly entered value; pair it with an [isEditable] of
     *   `true` and update the backing state from here so the next composition reflects the edit
     */
    public fun column(
        header: String,
        isEditable: Boolean = false,
        onCellEdit: (row: R, rowIndex: Int, newValue: Any?) -> Unit = { _, _, _ -> },
        value: (row: R) -> Any?,
    )
}

/** One declared column: its header, value extractor, editability, and edit callback. */
private class ColumnDeclaration<R>(
    val header: String,
    val isEditable: Boolean,
    val onCellEdit: (row: R, rowIndex: Int, newValue: Any?) -> Unit,
    val value: (row: R) -> Any?,
)

private class TableScopeImpl<R> : TableScope<R> {
    val columns: MutableList<ColumnDeclaration<R>> = ArrayList()

    override fun column(
        header: String,
        isEditable: Boolean,
        onCellEdit: (row: R, rowIndex: Int, newValue: Any?) -> Unit,
        value: (row: R) -> Any?,
    ) {
        columns.add(ColumnDeclaration(header, isEditable, onCellEdit, value))
    }
}

/**
 * The [AbstractTableModel] backing a [Table]: it presents [rows] through the [columns]' value
 * extractors and routes a committed cell edit to the edited column's `onCellEdit`.
 *
 * [refresh] takes the latest rows and columns on every recomposition and fires the *narrowest*
 * change event the difference warrants: a structure change only when the column shape (count,
 * headers, editability) differs, a data change otherwise. Firing a data change rather than a
 * structure change for a rows-only update is what keeps the table's selection intact across a
 * routine data refresh, so a controlled selection does not get reset and re-applied on every pass.
 * An in-place edit never mutates [rows] itself, so the displayed value only changes once the caller
 * updates the backing state and a new composition supplies fresh rows.
 */
private class TableModel<R> : AbstractTableModel() {
    private var rows: List<R> = emptyList()
    private var columns: List<ColumnDeclaration<R>> = emptyList()

    /** Pushes the latest data into the model and returns `true` if the column structure changed. */
    fun refresh(
        rows: List<R>,
        columns: List<ColumnDeclaration<R>>,
    ): Boolean {
        val structureChanged = columnsDiffer(this.columns, columns)
        val rowsChanged = this.rows != rows
        this.rows = rows
        this.columns = columns
        // Fire the narrowest event for the actual difference, and nothing when nothing changed: a
        // structure change rebuilds columns (and clears selection), a data change repaints cells
        // while leaving selection intact, and an unchanged refresh stays silent so a routine
        // recomposition never disturbs the table or its selection.
        when {
            structureChanged -> fireTableStructureChanged()
            rowsChanged -> fireTableDataChanged()
        }
        return structureChanged
    }

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = columns.size

    override fun getColumnName(column: Int): String = columns[column].header

    override fun getValueAt(
        rowIndex: Int,
        columnIndex: Int,
    ): Any? = columns[columnIndex].value(rows[rowIndex])

    override fun isCellEditable(
        rowIndex: Int,
        columnIndex: Int,
    ): Boolean = columns[columnIndex].isEditable

    override fun setValueAt(
        aValue: Any?,
        rowIndex: Int,
        columnIndex: Int,
    ) {
        columns[columnIndex].onCellEdit(rows[rowIndex], rowIndex, aValue)
    }

    private companion object {
        /**
         * Whether two column declarations describe a different table structure. The value/edit
         * lambdas are rebuilt every composition and so are never reference-equal; comparing only the
         * structural fields (count, header, editability) keeps a routine recomposition from being
         * mistaken for a structure change.
         */
        fun columnsDiffer(
            old: List<ColumnDeclaration<*>>,
            new: List<ColumnDeclaration<*>>,
        ): Boolean {
            if (old.size != new.size) return true
            return old.indices.any { i ->
                old[i].header != new[i].header || old[i].isEditable != new[i].isEditable
            }
        }
    }
}

/**
 * Re-applies [indices] as the table's selected rows, but only when it differs from the current
 * selection.
 *
 * A guard against re-setting an unchanged selection keeps a programmatic set from echoing back
 * through the selection listener as a spurious `onSelectionChange`.
 */
private fun applySelection(
    table: JTable,
    indices: List<Int>,
) {
    val rowCount = table.rowCount
    val valid = indices.filter { it in 0 until rowCount }
    if (table.selectedRows.toList() == valid) return
    val selectionModel = table.selectionModel
    selectionModel.valueIsAdjusting = true
    table.clearSelection()
    for (index in valid) selectionModel.addSelectionInterval(index, index)
    selectionModel.valueIsAdjusting = false
}
