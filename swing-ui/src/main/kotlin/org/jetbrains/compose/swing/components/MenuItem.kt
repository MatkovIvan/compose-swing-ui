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
import javax.swing.JMenuItem

/**
 * A composable wrapper for JMenuItem.
 *
 * @param text the text of the menu item
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param onClick callback to be invoked when the menu item is clicked
 */
@Composable
@SwingMenuComposable
public fun MenuItem(
    text: String,
    modifier: SwingModifier = SwingModifier,
    onClick: () -> Unit = {},
) {
    val callback = rememberUpdatedState(onClick)
    val listener = remember { ActionListener { callback.value() } }
    MenuItem(text = text, actionListener = listener, modifier = modifier)
}

/**
 * A JMenuItem driven by a raw [ActionListener] instead of an `onClick` lambda. The listener is
 * attached as-is and removed on the same instance; pass a stable instance (e.g. `remember {}`) to
 * avoid churn.
 *
 * @param text the text of the menu item
 * @param actionListener the listener notified when the item is activated
 * @param modifier the [SwingModifier] applied to the underlying component
 */
@Composable
@SwingMenuComposable
public fun MenuItem(
    text: String,
    actionListener: ActionListener,
    modifier: SwingModifier = SwingModifier,
) {
    MenuNode(
        factory = { JMenuItem() },
        update = {
            set(text) { this.text = it }
            applyModifier(
                SwingModifier
                    .listener<JMenuItem, ActionListener>(
                        actionListener,
                        { c, l -> c.addActionListener(l) },
                        { c, l -> c.removeActionListener(l) },
                    ) then modifier,
            )
        },
    )
}
