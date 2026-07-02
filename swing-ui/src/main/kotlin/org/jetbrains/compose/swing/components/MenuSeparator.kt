@file:JvmMultifileClass
@file:JvmName("MenuComponentsKt")

package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.MenuNode
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import javax.swing.JSeparator

/**
 * A composable wrapper for JSeparator in menus.
 *
 * @param modifier the [SwingModifier] applied to the underlying component
 */
@Composable
public fun MenuSeparator(modifier: SwingModifier = SwingModifier) {
    MenuNode(
        factory = { JSeparator() },
        update = {
            applyModifier(modifier)
        },
    )
}
