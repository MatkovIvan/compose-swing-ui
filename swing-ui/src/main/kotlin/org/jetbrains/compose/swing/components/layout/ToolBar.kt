@file:JvmMultifileClass
@file:JvmName("LayoutComponentsKt")

package org.jetbrains.compose.swing.components.layout

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.constants.ToolBarOrientation
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import javax.swing.JToolBar
import javax.swing.SwingConstants

/**
 * A composable wrapper for `JToolBar`, hosting a row or column of items.
 *
 * The items declared in [content] become the tool bar's children in declaration order:
 * ```
 * ToolBar {
 *     Button(text = "New", onClick = { … })
 *     Button(text = "Open", onClick = { … })
 * }
 * ```
 *
 * @param modifier the [SwingModifier] applied to the underlying `JToolBar`
 * @param orientation the axis along which items are laid out (a [ToolBarOrientation] `SwingConstants`
 *   value)
 * @param floatable whether the user can drag the tool bar out into a floating window
 * @param content the items hosted by the tool bar
 */
@Composable
public fun ToolBar(
    modifier: SwingModifier = SwingModifier,
    @ToolBarOrientation orientation: Int = SwingConstants.HORIZONTAL,
    floatable: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    SwingNode(
        factory = { JToolBar(orientation) },
        update = {
            set(orientation) { this.orientation = it }
            set(floatable) { this.isFloatable = it }
            applyModifier(modifier)
        },
        content = content,
    )
}
