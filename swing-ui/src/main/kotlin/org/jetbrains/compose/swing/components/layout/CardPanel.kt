@file:JvmMultifileClass
@file:JvmName("LayoutComponentsKt")

package org.jetbrains.compose.swing.components.layout

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.modifier.SwingModifier
import java.awt.CardLayout

/**
 * A composable wrapper for JPanel with CardLayout.
 *
 * @param modifier the [SwingModifier] applied to the panel
 * @param hgap the horizontal gap
 * @param vgap the vertical gap
 * @param content the composable content of the panel
 */
@Composable
public fun CardPanel(
    modifier: SwingModifier = SwingModifier,
    hgap: Int = 0,
    vgap: Int = 0,
    content: @Composable () -> Unit = {},
) {
    Panel(
        modifier = modifier,
        layout = CardLayout(hgap, vgap),
        content = content,
    )
}
