@file:JvmMultifileClass
@file:JvmName("ComponentsKt")

package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.listener
import java.awt.event.ActionListener
import javax.swing.JComboBox

/**
 * A composable wrapper for JComboBox.
 *
 * @param items the list of items to display
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedIndex the index of the selected item
 * @param onSelectionChange callback invoked when the selection changes
 */
@Composable
public fun <T> ComboBox(
    items: List<T>,
    modifier: SwingModifier = SwingModifier,
    selectedIndex: Int = -1,
    onSelectionChange: (Int) -> Unit = {},
) {
    val callback = rememberUpdatedState(onSelectionChange)
    val listener = remember { ActionListener { event -> callback.value((event.source as JComboBox<*>).selectedIndex) } }
    ComboBox(items = items, actionListener = listener, modifier = modifier, selectedIndex = selectedIndex)
}

/**
 * A composable wrapper for JComboBox driven by a raw [ActionListener] instead of an `onSelectionChange`
 * lambda. The [actionListener] is attached as-is and removed on the same instance; pass a stable
 * instance (e.g. `remember {}`) to avoid churn.
 *
 * @param items the list of items to display
 * @param actionListener the listener notified when the selection changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedIndex the index of the selected item
 */
@Composable
public fun <T> ComboBox(
    items: List<T>,
    actionListener: ActionListener,
    modifier: SwingModifier = SwingModifier,
    selectedIndex: Int = -1,
) {
    SwingNode(
        factory = { JComboBox<T>() },
        update = {
            set(items) {
                this.removeAllItems()
                it.forEach { item -> this.addItem(item) }
            }
            set(selectedIndex) {
                if (it >= 0 && it < this.itemCount) {
                    this.selectedIndex = it
                }
            }
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
