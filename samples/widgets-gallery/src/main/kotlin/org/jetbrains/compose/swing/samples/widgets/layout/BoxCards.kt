package org.jetbrains.compose.swing.samples.widgets.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.layout.Panel
import org.jetbrains.compose.swing.components.selection.RadioGroup
import org.jetbrains.compose.swing.foundation.layout.Alignment
import org.jetbrains.compose.swing.foundation.layout.Box
import org.jetbrains.compose.swing.foundation.layout.ColumnScope
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.background
import org.jetbrains.compose.swing.modifier.appearance.horizontalAlignment
import org.jetbrains.compose.swing.modifier.appearance.lineBorder
import org.jetbrains.compose.swing.modifier.appearance.opaque
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import org.jetbrains.compose.swing.modifier.listener.componentListener
import org.jetbrains.compose.swing.samples.widgets.ExampleCard
import org.jetbrains.compose.swing.samples.widgets.SectionColumn
import org.jetbrains.compose.swing.samples.widgets.SectionHeading
import org.jetbrains.compose.swing.tooling.Preview
import java.awt.Color
import javax.swing.SwingConstants

// Box stacks its children on top of one another: contentAlignment places every one of them, a child
// overrides that with its own align, matchParentSize takes the box's whole extent without contributing
// to it, and zIndex reorders the stack without touching declaration order. Every card is driven from a
// control and prints the placement or extent the box gave, so what the control changed can be read off
// the card instead of measured against the border.
@Preview
@Composable
internal fun BoxSection() {
    SectionColumn {
        SectionHeading("Box")
        BoxContentAlignmentCard()
        BoxChildAlignCard()
        BoxMatchParentSizeCard()
        BoxZIndexCard()
    }
}

// A radio group drives the box's own contentAlignment through all nine positions; two children of
// different sizes make each one visible, since a bigger and a smaller child land differently within it.
@Composable
private fun ColumnScope.BoxContentAlignmentCard() {
    ExampleCard("Box (contentAlignment)") {
        val alignments =
            listOf(
                "TopStart" to Alignment.TopStart,
                "TopCenter" to Alignment.TopCenter,
                "TopEnd" to Alignment.TopEnd,
                "CenterStart" to Alignment.CenterStart,
                "Center" to Alignment.Center,
                "CenterEnd" to Alignment.CenterEnd,
                "BottomStart" to Alignment.BottomStart,
                "BottomCenter" to Alignment.BottomCenter,
                "BottomEnd" to Alignment.BottomEnd,
            )
        var selected by remember { mutableIntStateOf(4) }
        var frontX by remember { mutableIntStateOf(0) }
        var frontY by remember { mutableIntStateOf(0) }
        RadioGroup(selectedIndex = selected, onSelectionChange = { selected = it }) {
            alignments.forEach { (name, _) -> option(name) }
        }
        Label("contentAlignment = ${alignments[selected].first}, front child at ($frontX, $frontY) px")
        Box(
            modifier = SwingModifier.preferredSize(260, 140).lineBorder(Color.GRAY),
            contentAlignment = alignments[selected].second,
        ) {
            Label(
                "behind",
                modifier =
                    SwingModifier
                        .preferredSize(140, 80)
                        .background(Color(0xD8E4F0))
                        .opaque(true)
                        .horizontalAlignment(SwingConstants.CENTER),
            )
            Label(
                "front",
                modifier =
                    SwingModifier
                        .preferredSize(70, 32)
                        .background(Color(0xF6C99B))
                        .opaque(true)
                        .horizontalAlignment(SwingConstants.CENTER)
                        .componentListener(
                            onComponentMoved = {
                                frontX = it.component.x
                                frontY = it.component.y
                            },
                        ),
            )
        }
    }
}

// The checkbox moves the box's own contentAlignment between Center and BottomCenter. The four labels that
// name a corner with align stay where they are; the one label left plain is the only child the box is
// still free to place, and its printed position is what moves.
@Composable
private fun ColumnScope.BoxChildAlignCard() {
    ExampleCard("Box (child align overrides contentAlignment)") {
        var bottomCenter by remember { mutableStateOf(false) }
        var plainX by remember { mutableIntStateOf(0) }
        var plainY by remember { mutableIntStateOf(0) }
        CheckBox(
            text = "Box places unaligned children at BottomCenter",
            checked = bottomCenter,
            onCheckedChange = { bottomCenter = it },
        )
        Label("Unaligned child sits at ${if (bottomCenter) "BottomCenter" else "Center"}: ($plainX, $plainY) px")
        Box(
            modifier = SwingModifier.preferredSize(260, 140).lineBorder(Color.GRAY),
            contentAlignment = if (bottomCenter) Alignment.BottomCenter else Alignment.Center,
        ) {
            Label("align(TopStart)", modifier = SwingModifier.align(Alignment.TopStart))
            Label("align(TopEnd)", modifier = SwingModifier.align(Alignment.TopEnd))
            Label("align(BottomStart)", modifier = SwingModifier.align(Alignment.BottomStart))
            Label("align(BottomEnd)", modifier = SwingModifier.align(Alignment.BottomEnd))
            Label(
                "Plain child",
                modifier =
                    SwingModifier.componentListener(
                        onComponentMoved = {
                            plainX = it.component.x
                            plainY = it.component.y
                        },
                    ),
            )
        }
    }
}

// The label alone sizes the box; the panel behind it declares matchParentSize and fills whatever extent
// that leaves, without asking for any size of its own. Dropping the declaration leaves the panel with
// the little an empty panel asks for on its own, which is what the printed backdrop size falls to.
@Composable
private fun ColumnScope.BoxMatchParentSizeCard() {
    ExampleCard("Box (matchParentSize)") {
        var match by remember { mutableStateOf(true) }
        var backdropWidth by remember { mutableIntStateOf(0) }
        var backdropHeight by remember { mutableIntStateOf(0) }
        CheckBox(
            text = "Backdrop declares matchParentSize",
            checked = match,
            onCheckedChange = { match = it },
        )
        Label("Backdrop: $backdropWidth x $backdropHeight px")
        Box(modifier = SwingModifier.lineBorder(Color.GRAY)) {
            val backdrop =
                SwingModifier
                    .background(Color(0xD8E4F0))
                    .opaque(true)
                    .componentListener(
                        onComponentResized = {
                            backdropWidth = it.component.width
                            backdropHeight = it.component.height
                        },
                    )
            Panel(modifier = if (match) backdrop.matchParentSize() else backdrop) { }
            Label(
                "Sized by me alone",
                modifier = SwingModifier.preferredSize(220, 90).horizontalAlignment(SwingConstants.CENTER),
            )
        }
    }
}

// B and A tie at zIndex 0f, and children that tie stack in the reverse of declaration order, so A -
// declared last - starts on top. The checkbox raises B's zIndex above A's, which brings B to the front
// while A stays the last declared child, so what moves is the z-index and nothing else.
@Composable
private fun ColumnScope.BoxZIndexCard() {
    ExampleCard("Box (zIndex)") {
        var raiseB by remember { mutableStateOf(false) }
        CheckBox(text = "Raise B above A", checked = raiseB, onCheckedChange = { raiseB = it })
        Label("Front child: ${if (raiseB) "B" else "A"}")
        Box(modifier = SwingModifier.preferredSize(200, 140), contentAlignment = Alignment.Center) {
            Label(
                "B",
                modifier =
                    SwingModifier
                        .preferredSize(90, 140)
                        .background(Color(0xB9D8B0))
                        .opaque(true)
                        .horizontalAlignment(SwingConstants.CENTER)
                        .zIndex(if (raiseB) 1f else 0f),
            )
            Label(
                "A",
                modifier =
                    SwingModifier
                        .preferredSize(140, 90)
                        .background(Color(0xF6C99B))
                        .opaque(true)
                        .horizontalAlignment(SwingConstants.CENTER),
            )
        }
    }
}
