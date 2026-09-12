package org.jetbrains.compose.swing.samples.widgets.layout

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.components.ComboBox
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.accessibility.accessibleName
import org.jetbrains.compose.swing.modifier.appearance.background
import org.jetbrains.compose.swing.modifier.appearance.border
import org.jetbrains.compose.swing.modifier.appearance.contentAreaFilled
import org.jetbrains.compose.swing.modifier.appearance.cursor
import org.jetbrains.compose.swing.modifier.appearance.foreground
import org.jetbrains.compose.swing.modifier.appearance.horizontalAlignment
import org.jetbrains.compose.swing.modifier.appearance.opaque
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.SwingConstants

private val layoutBorder = SwingModifier.border(BorderFactory.createLineBorder(LayoutSampleColors.Border))

internal val layoutTrack = layoutBorder.opaque(true).background(LayoutSampleColors.Track)

internal fun SwingModifier.layoutSampleSurface(color: Color): SwingModifier =
    then(layoutBorder)
        .opaque(true)
        .background(color)
        .foreground(LayoutSampleColors.Text)

@Composable
internal fun LayoutSwatch(
    text: String,
    color: Color,
    modifier: SwingModifier = SwingModifier,
) {
    Label(
        text = text,
        modifier = modifier.layoutSampleSurface(color).horizontalAlignment(SwingConstants.CENTER),
    )
}

@Composable
internal fun InteractiveLayoutSwatch(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: SwingModifier = SwingModifier,
    cursor: Cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
) {
    Button(
        text = text,
        onClick = onClick,
        modifier =
            modifier
                .contentAreaFilled(false)
                .layoutSampleSurface(color)
                .cursor(cursor)
                .horizontalAlignment(SwingConstants.CENTER),
    )
}

@Composable
internal fun LayoutRegionSwatch(
    text: String,
    color: Color,
    modifier: SwingModifier = SwingModifier,
    width: Int = 0,
) {
    LayoutSwatch(text, color, modifier.preferredSize(Dimension(width, 28)))
}

@Composable
internal fun <T> LayoutParameterSelector(
    label: String,
    options: List<Pair<String, T>>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
) {
    val names = options.map { it.first }
    Label(label)
    ComboBox(
        items = names,
        selectedItem = names[selectedIndex],
        onSelectionChange = { selected ->
            val index = names.indexOf(selected)
            if (index >= 0) onSelectionChange(index)
        },
        modifier = SwingModifier.accessibleName(label),
    )
}
