@file:JvmMultifileClass
@file:JvmName("ComponentsKt")

package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.constants.HorizontalAlignment
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import javax.swing.JLabel
import javax.swing.SwingConstants

/**
 * A composable wrapper for JLabel.
 *
 * @param text the text to display
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param horizontalAlignment the horizontal alignment of the text (a [HorizontalAlignment]
 *   `SwingConstants` value)
 */
@Composable
public fun Label(
    text: String,
    modifier: SwingModifier = SwingModifier,
    @HorizontalAlignment horizontalAlignment: Int = SwingConstants.LEADING,
) {
    SwingNode(
        factory = { JLabel() },
        update = {
            set(text) { this.text = it }
            set(horizontalAlignment) { this.horizontalAlignment = it }
            applyModifier(modifier)
        },
    )
}
