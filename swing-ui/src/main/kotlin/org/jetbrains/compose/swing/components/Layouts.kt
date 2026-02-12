package org.jetbrains.compose.swing.components

import androidx.compose.runtime.*
import java.awt.*
import javax.swing.BoxLayout
import javax.swing.JPanel

/**
 * A composable wrapper for JPanel with customizable layout.
 *
 * @param layout the layout manager to use
 * @param background the background color
 * @param preferredSize the preferred size of the panel
 * @param content the composable content of the panel
 */
@Composable
fun Panel(
    layout: LayoutManager? = FlowLayout(),
    background: Color? = null,
    preferredSize: Dimension? = null,
    content: @Composable () -> Unit = {}
) {
    val panel = remember { JPanel() }
    
    ComposeNode<Component, androidx.compose.runtime.Applier<Any>>(
        factory = { panel },
        update = {
            set(layout) { panel.layout = it }
            set(background) { it?.let { panel.background = it } }
            set(preferredSize) { it?.let { panel.preferredSize = it } }
        }
    ) {
        content()
    }
}

/**
 * A composable wrapper for JPanel with FlowLayout.
 *
 * @param alignment the alignment (FlowLayout.LEFT, FlowLayout.CENTER, FlowLayout.RIGHT)
 * @param hgap the horizontal gap between components
 * @param vgap the vertical gap between components
 * @param background the background color
 * @param content the composable content of the panel
 */
@Composable
fun FlowPanel(
    alignment: Int = FlowLayout.CENTER,
    hgap: Int = 5,
    vgap: Int = 5,
    background: Color? = null,
    content: @Composable () -> Unit = {}
) {
    Panel(
        layout = FlowLayout(alignment, hgap, vgap),
        background = background,
        content = content
    )
}

/**
 * A composable wrapper for JPanel with BorderLayout.
 * 
 * Children are automatically assigned BorderLayout constraints based on their order:
 * 1st child -> NORTH, 2nd child -> CENTER, 3rd child -> SOUTH
 *
 * @param hgap the horizontal gap between components
 * @param vgap the vertical gap between components
 * @param background the background color
 * @param content the composable content of the panel
 */
@Composable
fun BorderPanel(
    hgap: Int = 0,
    vgap: Int = 0,
    background: Color? = null,
    content: @Composable () -> Unit = {}
) {
    Panel(
        layout = BorderLayout(hgap, vgap),
        background = background,
        content = content
    )
}

/**
 * A composable wrapper for JPanel with BoxLayout.
 *
 * @param axis the axis (BoxLayout.X_AXIS, BoxLayout.Y_AXIS, BoxLayout.LINE_AXIS, BoxLayout.PAGE_AXIS)
 * @param background the background color
 * @param content the composable content of the panel
 */
@Composable
fun BoxPanel(
    axis: Int = BoxLayout.Y_AXIS,
    background: Color? = null,
    content: @Composable () -> Unit = {}
) {
    val panel = remember { JPanel() }
    
    ComposeNode<Component, androidx.compose.runtime.Applier<Any>>(
        factory = { panel },
        update = {
            set(axis) { panel.layout = BoxLayout(panel, it) }
            set(background) { it?.let { panel.background = it } }
        }
    ) {
        content()
    }
}

/**
 * A composable wrapper for JPanel with GridLayout.
 *
 * @param rows the number of rows
 * @param cols the number of columns
 * @param hgap the horizontal gap between components
 * @param vgap the vertical gap between components
 * @param background the background color
 * @param content the composable content of the panel
 */
@Composable
fun GridPanel(
    rows: Int = 0,
    cols: Int = 0,
    hgap: Int = 0,
    vgap: Int = 0,
    background: Color? = null,
    content: @Composable () -> Unit = {}
) {
    Panel(
        layout = GridLayout(rows, cols, hgap, vgap),
        background = background,
        content = content
    )
}

/**
 * A composable wrapper for JPanel with GridBagLayout.
 *
 * @param background the background color
 * @param content the composable content of the panel
 */
@Composable
fun GridBagPanel(
    background: Color? = null,
    content: @Composable () -> Unit = {}
) {
    Panel(
        layout = GridBagLayout(),
        background = background,
        content = content
    )
}

/**
 * A composable wrapper for JPanel with CardLayout.
 *
 * @param hgap the horizontal gap
 * @param vgap the vertical gap
 * @param background the background color
 * @param content the composable content of the panel
 */
@Composable
fun CardPanel(
    hgap: Int = 0,
    vgap: Int = 0,
    background: Color? = null,
    content: @Composable () -> Unit = {}
) {
    Panel(
        layout = CardLayout(hgap, vgap),
        background = background,
        content = content
    )
}
