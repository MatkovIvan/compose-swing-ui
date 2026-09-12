package org.jetbrains.compose.swing.samples.widgets.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.Slider
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.foundation.layout.ColumnScope
import org.jetbrains.compose.swing.foundation.layout.Row
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.background
import org.jetbrains.compose.swing.modifier.appearance.border
import org.jetbrains.compose.swing.modifier.appearance.horizontalAlignment
import org.jetbrains.compose.swing.modifier.appearance.opaque
import org.jetbrains.compose.swing.modifier.layout.maximumSize
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import org.jetbrains.compose.swing.modifier.listener.componentListener
import org.jetbrains.compose.swing.samples.widgets.ExampleCard
import java.awt.Color
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.SwingConstants

// weight() cards outside a filled row: every row here keeps its own preferred width, so a weighted
// child's share shows up as room the row itself asks its parent for, not room the parent hands down.
// A gray outline on each row makes that width legible, a colored swatch stands in for the weighted
// child, and the width the layout actually granted is printed beside every card - the share a child
// declines to take, or is capped out of, is empty space no border can name on its own.
@Composable
internal fun ColumnScope.WeightCards() {
    WeightSharesCard()
    WeightFillCard()
    WeightMaximumSizeCard()
}

private val rowOutline = SwingModifier.border(BorderFactory.createLineBorder(Color.GRAY))

@Composable
private fun Swatch(
    text: String,
    color: Color,
    modifier: SwingModifier = SwingModifier,
) {
    Label(
        text = text,
        modifier = modifier.opaque(true).background(color).horizontalAlignment(SwingConstants.CENTER),
    )
}

// Both swatches ask for the same 50 px, so the width each weight unit implies is 50 px, and the row
// asks its parent for that much for every share it hands out. Sliding the second swatch's weight up is
// the row growing, not the parent giving: nothing outside the row changed.
@Composable
internal fun ColumnScope.WeightSharesCard() {
    ExampleCard("weight (shares set the row's own width)") {
        var weight by remember { mutableIntStateOf(3) }
        var rowWidth by remember { mutableIntStateOf(0) }
        Label("Second swatch weight: ${weight}f")
        Slider(value = weight, onValueChange = { weight = it }, min = 1, max = 5)
        Label("Row asks for: $rowWidth px")
        Row(modifier = rowOutline.componentListener(onComponentResized = { rowWidth = it.component.width })) {
            Swatch("1f", Color(0xC8, 0xE6, 0xC9), SwingModifier.weight(1f).preferredSize(Dimension(50, 28)))
            Swatch(
                "${weight}f",
                Color(0xFF, 0xE0, 0xB2),
                SwingModifier.weight(weight.toFloat()).preferredSize(Dimension(50, 28)),
            )
        }
    }
}

// Both children claim an equal 1f share of a row fixed at 300 px. A swatch that does not fill keeps only
// the width it prefers and leaves the rest of its share empty inside the outline; the one beside it takes
// the whole share it was granted, and the two printed widths are equal only once both fill.
@Composable
internal fun ColumnScope.WeightFillCard() {
    ExampleCard("weight (fill vs. fill = false)") {
        var fill by remember { mutableStateOf(false) }
        var rowWidth by remember { mutableIntStateOf(0) }
        var leftWidth by remember { mutableIntStateOf(0) }
        var rightWidth by remember { mutableIntStateOf(0) }
        CheckBox(text = "Left swatch fills its share", checked = fill, onCheckedChange = { fill = it })
        Label("Shares: left swatch $leftWidth px, right swatch $rightWidth px of the row's $rowWidth px")
        Row(
            modifier =
                rowOutline
                    .preferredSize(Dimension(300, 32))
                    .componentListener(onComponentResized = { rowWidth = it.component.width }),
        ) {
            Swatch(
                if (fill) "fill" else "no fill",
                Color(0xD1, 0xC4, 0xE9),
                SwingModifier
                    .weight(1f, fill = fill)
                    .preferredSize(Dimension(80, 28))
                    .componentListener(onComponentResized = { leftWidth = it.component.width }),
            )
            Swatch(
                "fill",
                Color(0xB3, 0xE5, 0xFC),
                SwingModifier
                    .weight(1f)
                    .preferredSize(Dimension(80, 28))
                    .componentListener(onComponentResized = { rightWidth = it.component.width }),
            )
        }
    }
}

// The swatch is the row's only child and would take the row's whole 300 px, but a maximumSize caps how
// much of that share it occupies. What the cap refuses stays empty inside the outline rather than going
// to another child, since there is none.
@Composable
internal fun ColumnScope.WeightMaximumSizeCard() {
    ExampleCard("weight + maximumSize") {
        var capped by remember { mutableStateOf(true) }
        var rowWidth by remember { mutableIntStateOf(0) }
        var swatchWidth by remember { mutableIntStateOf(0) }
        CheckBox(text = "Cap the swatch at 80 px", checked = capped, onCheckedChange = { capped = it })
        Label("Share taken: $swatchWidth px of the row's $rowWidth px")
        Row(
            modifier =
                rowOutline
                    .preferredSize(Dimension(300, 32))
                    .componentListener(onComponentResized = { rowWidth = it.component.width }),
        ) {
            val share =
                SwingModifier
                    .weight(1f)
                    .componentListener(onComponentResized = { swatchWidth = it.component.width })
            Swatch(
                if (capped) "capped" else "uncapped",
                Color(0xFF, 0xCC, 0xBC),
                if (capped) share.maximumSize(Dimension(80, 28)) else share,
            )
        }
    }
}
