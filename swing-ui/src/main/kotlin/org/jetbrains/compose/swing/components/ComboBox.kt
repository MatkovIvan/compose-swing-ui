@file:JvmMultifileClass
@file:JvmName("ComponentsKt")

package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.components.selection.ListItemScope
import org.jetbrains.compose.swing.components.selection.rememberComposingListCellRenderer
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.listener
import java.awt.event.ActionListener
import java.util.Vector
import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

/**
 * A composable wrapper for `JComboBox`.
 *
 * The selection is controlled via [selectedIndex] + [onSelectionChange], with `-1` meaning no
 * selection. Rebuilding [items] re-applies the declared selection without firing
 * [onSelectionChange]; only a change originating from the combo box fires the callback.
 *
 * By default each item renders its `toString`; supply [itemContent] to render an arbitrary composable
 * cell per item against a [ListItemScope].
 *
 * @param items the list of items to display
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedIndex the index of the selected item (controlled); `-1` selects nothing
 * @param onSelectionChange callback invoked when the selection changes
 * @param itemContent optional composable cell rendered per item against a [ListItemScope]; `null` keeps
 *   the default `toString` rendering
 */
@Composable
public fun <T> ComboBox(
    items: List<T>,
    modifier: SwingModifier = SwingModifier,
    selectedIndex: Int = -1,
    onSelectionChange: (Int) -> Unit = {},
    itemContent: (@Composable ListItemScope.(item: T) -> Unit)? = null,
) {
    ComboBox(
        items = items,
        actionListener = rememberSelectionListener(onSelectionChange),
        modifier = modifier,
        selectedIndex = selectedIndex,
        itemContent = itemContent,
    )
}

/**
 * A `ComboBox` driven by a raw [ActionListener] instead of an `onSelectionChange` lambda. The
 * [actionListener] is attached as-is and removed on the same instance; pass a stable instance (e.g.
 * `remember {}`) to avoid churn.
 *
 * By default each item renders its `toString`; supply [itemContent] to render an arbitrary composable
 * cell per item against a [ListItemScope].
 *
 * @param items the list of items to display
 * @param actionListener the listener notified when the selection changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedIndex the index of the selected item (controlled); `-1` selects nothing
 * @param itemContent optional composable cell rendered per item against a [ListItemScope]; `null` keeps
 *   the default `toString` rendering
 */
@Composable
public fun <T> ComboBox(
    items: List<T>,
    actionListener: ActionListener,
    modifier: SwingModifier = SwingModifier,
    selectedIndex: Int = -1,
    itemContent: (@Composable ListItemScope.(item: T) -> Unit)? = null,
) {
    // The single conversion from itemContent to the combo box cell renderer: one reused
    // ComposingListCellRenderer stamps a recycled composition per row. A null itemContent keeps the
    // JComboBox default toString renderer.
    val renderer = itemContent?.let { rememberComposingListCellRenderer(it) }
    SwingNode(
        factory = { JComboBox<T>() },
        update = {
            set(renderer) { it?.let { r -> this.renderer = r } }
            set(items) { newItems ->
                // A prebuilt model already carrying the declared selection swaps in silently
                // (setModel fires no action event); mutating the live model instead would echo the
                // transient deselection and first-item auto-selection through the action listener.
                val model = DefaultComboBoxModel(Vector(newItems))
                model.selectedItem = newItems.getOrNull(selectedIndex)
                this.model = model
            }
            set(selectedIndex) { applySelection(this, it) }
            applyModifier(
                SwingModifier
                    .listener<JComboBox<*>, ActionListener>(
                        actionListener,
                        { c, l -> c.addActionListener(l) },
                        { c, l -> c.removeActionListener(l) },
                    ) then modifier,
            )
        },
    )
}

/**
 * A `ComboBox` driven by a caller-owned [ComboBoxModel]. The model owns both the items and the
 * selection, so this overload is observation-only: it renders the model and reports selection changes
 * through [onSelectionChange] without ever writing the selection back into the model. Swapping the
 * [model] instance installs the new model verbatim.
 *
 * @param model the combo box model to render; owns its items and selection
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param onSelectionChange callback invoked with the settled selected index when the selection changes
 */
@Composable
public fun <T> ComboBox(
    model: ComboBoxModel<T>,
    modifier: SwingModifier = SwingModifier,
    onSelectionChange: (Int) -> Unit = {},
) {
    ComboBox(model = model, actionListener = rememberSelectionListener(onSelectionChange), modifier = modifier)
}

/**
 * A model-driven `ComboBox` driven by a raw [ActionListener] instead of an `onSelectionChange` lambda.
 * The [model] owns its items and selection and is never mutated by the library; the [actionListener]
 * is attached as-is and removed on the same instance, so pass a stable instance (e.g. `remember {}`)
 * to avoid churn. Swapping the [model] instance installs the new model verbatim.
 *
 * @param model the combo box model to render; owns its items and selection
 * @param actionListener the listener notified when the selection changes
 * @param modifier the [SwingModifier] applied to the underlying component
 */
@Composable
public fun <T> ComboBox(
    model: ComboBoxModel<T>,
    actionListener: ActionListener,
    modifier: SwingModifier = SwingModifier,
) {
    SwingNode(
        factory = { JComboBox<T>() },
        update = {
            set(model) { this.model = it }
            applyModifier(
                SwingModifier
                    .listener<JComboBox<*>, ActionListener>(
                        actionListener,
                        { c, l -> c.addActionListener(l) },
                        { c, l -> c.removeActionListener(l) },
                    ) then modifier,
            )
        },
    )
}

/**
 * Remembers a stable [ActionListener] that reads back the settled `selectedIndex` from the event's
 * combo box and forwards it to [onSelectionChange]. The listener instance is stable across
 * recompositions so it attaches and detaches on the same object, while the current
 * [onSelectionChange] is tracked through [rememberUpdatedState] so the latest callback is invoked.
 */
@Composable
private fun rememberSelectionListener(onSelectionChange: (Int) -> Unit): ActionListener {
    val callback = rememberUpdatedState(onSelectionChange)
    return remember { ActionListener { event -> callback.value((event.source as JComboBox<*>).selectedIndex) } }
}

/**
 * Re-applies [index] as the combo box's selection, coercing an out-of-range index to `-1` (no
 * selection), but only when it differs from the current selection.
 *
 * `JComboBox.setSelectedIndex` fires the action listener even when the selection is unchanged, so
 * the guard against re-setting an unchanged selection is what keeps a programmatic set from echoing
 * back through it as a spurious `onSelectionChange`.
 */
private fun applySelection(
    comboBox: JComboBox<*>,
    index: Int,
) {
    val valid = if (index in 0 until comboBox.itemCount) index else -1
    if (comboBox.selectedIndex == valid) return
    comboBox.selectedIndex = valid
}
