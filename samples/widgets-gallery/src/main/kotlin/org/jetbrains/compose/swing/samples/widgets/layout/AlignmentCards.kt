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
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.foundation.layout.AbsoluteAlignment
import org.jetbrains.compose.swing.foundation.layout.Alignment
import org.jetbrains.compose.swing.foundation.layout.Arrangement
import org.jetbrains.compose.swing.foundation.layout.BiasAlignment
import org.jetbrains.compose.swing.foundation.layout.Box
import org.jetbrains.compose.swing.foundation.layout.Column
import org.jetbrains.compose.swing.foundation.layout.ColumnScope
import org.jetbrains.compose.swing.foundation.layout.Row
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.font
import org.jetbrains.compose.swing.modifier.appearance.lineBorder
import org.jetbrains.compose.swing.modifier.layout.componentOrientation
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import org.jetbrains.compose.swing.modifier.listener.componentListener
import org.jetbrains.compose.swing.samples.widgets.ExampleCard
import org.jetbrains.compose.swing.samples.widgets.SectionColumn
import org.jetbrains.compose.swing.samples.widgets.SectionHeading
import org.jetbrains.compose.swing.tooling.Preview
import java.awt.Color
import java.awt.ComponentOrientation
import java.awt.Dimension
import java.awt.Font

// What RowColumnLayoutCards leaves out: how a weighted child's share is granted, capped and filled, the
// relative arrangements and alignments against their orientation-blind Absolute counterparts, a bias slid
// continuously instead of picked from the nine named constants, and the shared baseline a label and a
// differently-sized field sit on. Each card prints the coordinate its control moves, so a placement that
// mirrors can be told from one that holds without measuring against the border.
@Preview
@Composable
internal fun WeightAndAlignmentSection() {
    SectionColumn {
        SectionHeading("Weight & alignment")
        WeightCards()
        ArrangementAbsoluteCard()
        AbsoluteAlignmentCard()
        BiasAlignmentCard()
        AlignByBaselineCard()
    }
}

// Arrangement.End reads the row's ComponentOrientation and mirrors under right-to-left; Absolute.Right
// always packs against the physical right edge. Both rows are given the same orientation, so flipping it
// moves the leading child of the first row and leaves the second row's exactly where it was.
@Composable
internal fun ColumnScope.ArrangementAbsoluteCard() {
    ExampleCard("Arrangement.End vs Arrangement.Absolute.Right") {
        var rightToLeft by remember { mutableStateOf(false) }
        var endX by remember { mutableIntStateOf(0) }
        var absoluteX by remember { mutableIntStateOf(0) }
        CheckBox(
            text = "Right-to-left orientation (arrangement)",
            checked = rightToLeft,
            onCheckedChange = { rightToLeft = it },
        )
        val orientation = if (rightToLeft) ComponentOrientation.RIGHT_TO_LEFT else ComponentOrientation.LEFT_TO_RIGHT
        Label("End: leading child at x = $endX px")
        Row(
            modifier = SwingModifier.fillWidth().lineBorder(Color.GRAY).componentOrientation(orientation),
            horizontalArrangement = Arrangement.End,
        ) {
            Label("One", modifier = SwingModifier.componentListener(onComponentMoved = { endX = it.component.x }))
            Label("Two")
        }
        Label("Absolute.Right: leading child at x = $absoluteX px")
        Row(
            modifier = SwingModifier.fillWidth().lineBorder(Color.GRAY).componentOrientation(orientation),
            horizontalArrangement = Arrangement.Absolute.Right,
        ) {
            Label("One", modifier = SwingModifier.componentListener(onComponentMoved = { absoluteX = it.component.x }))
            Label("Two")
        }
    }
}

// The same contrast for cross-axis alignment: Alignment.Start mirrors to the column's trailing edge under
// right-to-left, AbsoluteAlignment.Left never does.
@Composable
internal fun ColumnScope.AbsoluteAlignmentCard() {
    ExampleCard("Alignment.Start vs AbsoluteAlignment.Left") {
        var rightToLeft by remember { mutableStateOf(false) }
        var startX by remember { mutableIntStateOf(0) }
        var leftX by remember { mutableIntStateOf(0) }
        CheckBox(
            text = "Right-to-left orientation (alignment)",
            checked = rightToLeft,
            onCheckedChange = { rightToLeft = it },
        )
        val orientation = if (rightToLeft) ComponentOrientation.RIGHT_TO_LEFT else ComponentOrientation.LEFT_TO_RIGHT
        Label("Start: child at x = $startX px")
        Column(
            modifier =
                SwingModifier
                    .preferredSize(Dimension(220, 28))
                    .lineBorder(Color.GRAY)
                    .componentOrientation(orientation),
            horizontalAlignment = Alignment.Start,
        ) {
            Label(
                "Start child",
                modifier = SwingModifier.componentListener(onComponentMoved = { startX = it.component.x }),
            )
        }
        Label("AbsoluteAlignment.Left: child at x = $leftX px")
        Column(
            modifier =
                SwingModifier
                    .preferredSize(Dimension(220, 28))
                    .lineBorder(Color.GRAY)
                    .componentOrientation(orientation),
            horizontalAlignment = AbsoluteAlignment.Left,
        ) {
            Label(
                "Left child",
                modifier = SwingModifier.componentListener(onComponentMoved = { leftX = it.component.x }),
            )
        }
    }
}

// A BiasAlignment built from an arbitrary float, not one of the nine named constants: the slider moves
// the child continuously through the box rather than snapping between fixed positions, and the child's
// own x follows it a pixel at a time.
@Composable
internal fun ColumnScope.BiasAlignmentCard() {
    ExampleCard("BiasAlignment (arbitrary bias)") {
        var bias by remember { mutableIntStateOf(0) }
        var childX by remember { mutableIntStateOf(0) }
        Label("Horizontal bias: ${bias / 100f}, child at x = $childX px")
        Slider(value = bias, onValueChange = { bias = it }, min = -100, max = 100)
        Box(
            modifier = SwingModifier.preferredSize(Dimension(300, 40)).lineBorder(Color.GRAY),
            contentAlignment = BiasAlignment(bias / 100f, 0f),
        ) {
            Label(
                "Slides with the bias",
                modifier = SwingModifier.componentListener(onComponentMoved = { childX = it.component.x }),
            )
        }
    }
}

// The case alignByBaseline exists for: a label and a field at different font sizes, whose text only lines
// up when both sit on the row's shared baseline instead of at its own verticalAlignment. Turning it off
// drops the label onto the row's Bottom alignment, which its printed y follows.
@Composable
internal fun ColumnScope.AlignByBaselineCard() {
    ExampleCard("RowScope.alignByBaseline") {
        var baseline by remember { mutableStateOf(true) }
        var labelY by remember { mutableIntStateOf(0) }
        CheckBox(text = "Align by baseline", checked = baseline, onCheckedChange = { baseline = it })
        Label("Align by baseline: ${if (baseline) "on" else "off"}, label sits at y = $labelY px")
        var text by remember { mutableStateOf("Text field") }
        Row(
            modifier = SwingModifier.fillWidth(),
            horizontalArrangement = Arrangement.spacedBy(8),
            verticalAlignment = Alignment.Bottom,
        ) {
            val labelModifier =
                SwingModifier
                    .font(Font(Font.SANS_SERIF, Font.PLAIN, 12))
                    .componentListener(onComponentMoved = { labelY = it.component.y })
            Label(text = "Label:", modifier = if (baseline) labelModifier.alignByBaseline() else labelModifier)
            val fieldModifier = SwingModifier.font(Font(Font.SANS_SERIF, Font.PLAIN, 28))
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = if (baseline) fieldModifier.alignByBaseline() else fieldModifier,
                columns = 12,
            )
        }
    }
}
