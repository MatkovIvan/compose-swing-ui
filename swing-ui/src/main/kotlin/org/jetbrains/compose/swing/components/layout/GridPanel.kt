@file:JvmMultifileClass
@file:JvmName("LayoutComponentsKt")

package org.jetbrains.compose.swing.components.layout

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.modifier.SwingModifier
import java.awt.GridLayout

/**
 * A composable wrapper for JPanel with GridLayout.
 *
 * @param modifier the [SwingModifier] applied to the panel
 * @param rows the number of rows
 * @param cols the number of columns
 * @param hgap the horizontal gap between components
 * @param vgap the vertical gap between components
 * @param content the composable content of the panel
 */
@Composable
public fun GridPanel(
    modifier: SwingModifier = SwingModifier,
    rows: Int = 1,
    cols: Int = 0,
    hgap: Int = 0,
    vgap: Int = 0,
    content: @Composable () -> Unit = {},
) {
    Panel(
        modifier = modifier,
        layout = GridLayout(rows, cols, hgap, vgap),
        content = content,
    )
}
