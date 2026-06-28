@file:JvmMultifileClass
@file:JvmName("LayoutComponentsKt")

package org.jetbrains.compose.swing.components.layout

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.constants.BoxAxis
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import javax.swing.BoxLayout
import javax.swing.JPanel

/**
 * A composable wrapper for JPanel with BoxLayout.
 *
 * The [axis] is fixed at first composition.
 *
 * @param modifier the [SwingModifier] applied to the panel
 * @param axis the axis along which children are arranged (a [BoxAxis] `BoxLayout` value)
 * @param content the composable content of the panel
 */
@Composable
public fun BoxPanel(
    modifier: SwingModifier = SwingModifier,
    @BoxAxis axis: Int = BoxLayout.Y_AXIS,
    content: @Composable () -> Unit = {},
) {
    SwingNode(
        factory = { JPanel().apply { layout = BoxLayout(this, axis) } },
        update = {
            applyModifier(modifier)
        },
        content = content,
    )
}
