@file:JvmMultifileClass
@file:JvmName("LayoutComponentsKt")

package org.jetbrains.compose.swing.components.layout

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.modifier.SwingModifier
import java.awt.GridBagLayout

/**
 * A composable wrapper for JPanel with GridBagLayout.
 *
 * @param modifier the [SwingModifier] applied to the panel
 * @param content the composable content of the panel
 */
@Composable
public fun GridBagPanel(
    modifier: SwingModifier = SwingModifier,
    content: @Composable () -> Unit = {},
) {
    Panel(
        modifier = modifier,
        layout = GridBagLayout(),
        content = content,
    )
}
