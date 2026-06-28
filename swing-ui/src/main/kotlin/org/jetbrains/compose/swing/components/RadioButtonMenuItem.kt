@file:JvmMultifileClass
@file:JvmName("MenuComponentsKt")

package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.annotations.SwingMenuComposable
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.listener
import java.awt.event.ActionListener
import javax.swing.JRadioButtonMenuItem

/**
 * A composable wrapper for JRadioButtonMenuItem.
 *
 * @param text the text of the menu item
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selected whether the menu item is selected
 * @param onSelect callback invoked when the menu item is selected
 */
@Composable
@SwingMenuComposable
public fun RadioButtonMenuItem(
    text: String,
    modifier: SwingModifier = SwingModifier,
    selected: Boolean = false,
    onSelect: () -> Unit = {},
) {
    val callback = rememberUpdatedState(onSelect)
    val listener =
        remember { ActionListener { event -> if ((event.source as JRadioButtonMenuItem).isSelected) callback.value() } }
    RadioButtonMenuItem(text = text, actionListener = listener, modifier = modifier, selected = selected)
}

/**
 * A JRadioButtonMenuItem driven by a raw [ActionListener] instead of an `onSelect` lambda. The listener
 * is attached as-is and removed on the same instance; pass a stable instance (e.g. `remember {}`) to
 * avoid churn.
 *
 * @param text the text of the menu item
 * @param actionListener the listener notified when the item is activated
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selected whether the menu item is selected
 */
@Composable
@SwingMenuComposable
public fun RadioButtonMenuItem(
    text: String,
    actionListener: ActionListener,
    modifier: SwingModifier = SwingModifier,
    selected: Boolean = false,
) {
    MenuNode(
        factory = { JRadioButtonMenuItem() },
        update = {
            set(text) { this.text = it }
            set(selected) { this.isSelected = it }
            applyModifier(
                SwingModifier
                    .listener<JRadioButtonMenuItem, ActionListener>(
                        actionListener,
                        { c, l -> c.addActionListener(l) },
                        { c, l -> c.removeActionListener(l) },
                    ) then modifier,
            )
        },
    )
}
