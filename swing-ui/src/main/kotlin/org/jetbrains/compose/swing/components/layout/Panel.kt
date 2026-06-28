@file:JvmMultifileClass
@file:JvmName("LayoutComponentsKt")

package org.jetbrains.compose.swing.components.layout

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import java.awt.FlowLayout
import java.awt.LayoutManager
import javax.swing.JPanel

/**
 * A composable wrapper for JPanel with a fixed layout manager.
 *
 * The [layout] is fixed at first composition. To use a different layout, choose the dedicated panel
 * for it (e.g. [FlowPanel], [BorderPanel], [GridPanel]).
 *
 * Style it (background, border, preferred size, …) through [modifier].
 *
 * @param modifier the [SwingModifier] applied to the panel
 * @param layout the layout manager to use; fixed at first composition
 * @param content the composable content of the panel
 */
@Composable
public fun Panel(
    modifier: SwingModifier = SwingModifier,
    layout: LayoutManager = FlowLayout(),
    content: @Composable () -> Unit = {},
) {
    SwingNode(
        factory = { JPanel(layout) },
        update = {
            applyModifier(modifier)
        },
        content = content,
    )
}
