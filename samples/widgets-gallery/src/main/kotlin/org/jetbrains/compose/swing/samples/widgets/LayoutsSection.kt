package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.components.layout.BoxPanel
import org.jetbrains.compose.swing.components.layout.CardPanel
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.components.layout.GridBagPanel
import org.jetbrains.compose.swing.components.layout.GridPanel
import org.jetbrains.compose.swing.components.layout.Panel
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.alignmentX
import org.jetbrains.compose.swing.modifier.appearance.background
import org.jetbrains.compose.swing.modifier.appearance.opaque
import org.jetbrains.compose.swing.modifier.componentOrientation
import org.jetbrains.compose.swing.modifier.preferredSize
import java.awt.BorderLayout
import java.awt.Color
import java.awt.ComponentOrientation
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.SwingConstants

/**
 * Demonstrates every layout wrapper, including both [BorderPanel] region families (absolute compass
 * and orientation-aware) and a [CardPanel] whose visible card is driven by state.
 */
@Composable
internal fun LayoutsSection() {
    SectionColumn {
        SectionHeading("Layouts")
        PanelCard()
        FlowPanelCard()
        BorderCompassCard()
        BorderOrientationCard()
        BoxPanelCard()
        GridPanelCard()
        GridBagPanelCard()
        CardPanelCard()
    }
}

/** Panel with an explicit AWT [BorderLayout] passed through the generic [Panel] wrapper. */
@Composable
private fun PanelCard() {
    ExampleCard("Panel (explicit LayoutManager)") {
        Panel(layout = BorderLayout()) {
            Label("Single child in a BorderLayout-backed Panel")
        }
    }
}

/** FlowPanel with a leading alignment and custom gaps. */
@Composable
private fun FlowPanelCard() {
    ExampleCard("FlowPanel") {
        FlowPanel(alignment = SwingConstants.LEADING, hgap = 12, vgap = 4) {
            Button("One")
            Button("Two")
            Button("Three")
        }
    }
}

/** BorderPanel using the absolute compass family. */
@Composable
private fun BorderCompassCard() {
    ExampleCard("BorderPanel (compass regions)") {
        BorderPanel(modifier = SwingModifier.preferredSize(Dimension(360, 140)), hgap = 4, vgap = 4) {
            north { RegionLabel("north", Color(0xBB, 0xDE, 0xFB)) }
            south { RegionLabel("south", Color(0xC8, 0xE6, 0xC9)) }
            west { RegionLabel("west", Color(0xFF, 0xE0, 0xB2), width = EDGE_WIDTH) }
            east { RegionLabel("east", Color(0xF8, 0xBB, 0xD0), width = EDGE_WIDTH) }
            center { RegionLabel("center", Color(0xE0, 0xE0, 0xE0)) }
        }
    }
}

/**
 * BorderPanel using the orientation-aware family, with a state-driven RTL toggle. Flipping the
 * checkbox swaps the component orientation, which moves the `lineStart`/`lineEnd` children between
 * the leading and trailing edges live — `lineStart` resolves to the right edge under RTL and the
 * left edge under LTR.
 */
@Composable
private fun BorderOrientationCard() {
    ExampleCard("BorderPanel (orientation-aware)") {
        var rtl by remember { mutableStateOf(false) }
        CheckBox(
            text = "Right-to-left orientation",
            checked = rtl,
            onCheckedChange = { rtl = it },
        )
        BorderPanel(
            modifier =
                SwingModifier
                    .preferredSize(Dimension(360, 120))
                    .alignmentX(LEFT_ALIGNED)
                    .componentOrientation(
                        if (rtl) ComponentOrientation.RIGHT_TO_LEFT else ComponentOrientation.LEFT_TO_RIGHT,
                    ),
            hgap = 4,
            vgap = 4,
        ) {
            pageStart { RegionLabel("pageStart", Color(0xBB, 0xDE, 0xFB)) }
            pageEnd { RegionLabel("pageEnd", Color(0xC8, 0xE6, 0xC9)) }
            lineStart { RegionLabel("lineStart (leading)", Color(0xFF, 0xE0, 0xB2), width = EDGE_WIDTH) }
            lineEnd { RegionLabel("lineEnd (trailing)", Color(0xF8, 0xBB, 0xD0), width = EDGE_WIDTH) }
            center { RegionLabel("center", Color(0xE0, 0xE0, 0xE0)) }
        }
    }
}

/** BoxPanel stacking children along the Y axis. */
@Composable
private fun BoxPanelCard() {
    ExampleCard("BoxPanel (Y axis)") {
        BoxPanel(axis = BoxLayout.Y_AXIS) {
            Label("First")
            Label("Second")
            Label("Third")
        }
    }
}

/** GridPanel laying buttons into a fixed grid. */
@Composable
private fun GridPanelCard() {
    ExampleCard("GridPanel (2x3)") {
        GridPanel(rows = 2, cols = 3, hgap = 6, vgap = 6) {
            repeat(6) { index -> Button("Cell ${index + 1}") }
        }
    }
}

/** GridBagPanel with children placed through AWT's GridBagConstraints (the BorderLayout default cell). */
@Composable
private fun GridBagPanelCard() {
    ExampleCard("GridBagPanel") {
        GridBagPanel {
            Label("GridBagPanel hosts components positioned by GridBagLayout")
        }
    }
}

/** CardPanel showing one of several stacked cards; the index is controlled by state. */
@Composable
private fun CardPanelCard() {
    ExampleCard("CardPanel") {
        var card by remember { mutableIntStateOf(0) }
        FlowPanel {
            Button("Show A", onClick = { card = 0 })
            Button("Show B", onClick = { card = 1 })
            Button("Show C", onClick = { card = 2 })
        }
        CardPanel(modifier = SwingModifier.preferredSize(Dimension(320, 60))) {
            when (card) {
                0 -> RegionLabel("Card A", Color(0xBB, 0xDE, 0xFB))
                1 -> RegionLabel("Card B", Color(0xC8, 0xE6, 0xC9))
                else -> RegionLabel("Card C", Color(0xFF, 0xE0, 0xB2))
            }
        }
    }
}

/**
 * A filled, centered label used to make each layout region visible. The [width] sets the preferred
 * width: regions sized by their parent's full width (north/south/center) leave it at `0`, while the
 * horizontal-edge regions (west/east, lineStart/lineEnd) need a real width to claim space.
 */
@Composable
private fun RegionLabel(
    text: String,
    color: Color,
    width: Int = 0,
) {
    Label(
        text = text,
        modifier =
            SwingModifier
                .opaque(true)
                .background(color)
                .preferredSize(Dimension(width, REGION_HEIGHT)),
        horizontalAlignment = SwingConstants.CENTER,
    )
}

private const val REGION_HEIGHT = 28
private const val EDGE_WIDTH = 120
