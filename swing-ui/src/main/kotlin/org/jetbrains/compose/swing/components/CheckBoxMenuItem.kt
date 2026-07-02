@file:JvmMultifileClass
@file:JvmName("MenuComponentsKt")

package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.MenuNode
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.listener
import java.awt.event.ActionListener
import javax.swing.JCheckBoxMenuItem

/**
 * A composable wrapper for JCheckBoxMenuItem.
 *
 * @param text the text of the menu item
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param checked whether the menu item is checked
 * @param onCheckedChange callback invoked when the checked state changes
 */
@Composable
public fun CheckBoxMenuItem(
    text: String,
    modifier: SwingModifier = SwingModifier,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    val callback = rememberUpdatedState(onCheckedChange)
    val listener =
        remember { ActionListener { event -> callback.value((event.source as JCheckBoxMenuItem).isSelected) } }
    CheckBoxMenuItem(text = text, actionListener = listener, modifier = modifier, checked = checked)
}

/**
 * A JCheckBoxMenuItem driven by a raw [ActionListener] instead of an `onCheckedChange` lambda. The
 * listener is attached as-is and removed on the same instance; pass a stable instance (e.g.
 * `remember {}`) to avoid churn.
 *
 * @param text the text of the menu item
 * @param actionListener the listener notified when the item is toggled
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param checked whether the menu item is checked
 */
@Composable
public fun CheckBoxMenuItem(
    text: String,
    actionListener: ActionListener,
    modifier: SwingModifier = SwingModifier,
    checked: Boolean = false,
) {
    MenuNode(
        factory = { JCheckBoxMenuItem() },
        update = {
            set(text) { this.text = it }
            set(checked) { this.isSelected = it }
            applyModifier(
                SwingModifier
                    .listener<JCheckBoxMenuItem, ActionListener>(
                        actionListener,
                        { c, l -> c.addActionListener(l) },
                        { c, l -> c.removeActionListener(l) },
                    ) then modifier,
            )
        },
    )
}
