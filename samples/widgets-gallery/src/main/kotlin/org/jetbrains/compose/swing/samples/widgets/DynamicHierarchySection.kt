package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.Slider
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.layout.alignmentX
import org.jetbrains.compose.swing.modifier.layout.visible

/**
 * Demonstrates a state-driven dynamic hierarchy: a toggle conditionally composes an entire panel
 * subtree, so flipping it inserts or removes that subtree from the component tree — contrasted with
 * the `visible()` modifier, which keeps the slot in place and merely hides it.
 */
@Composable
internal fun DynamicHierarchySection() {
    SectionColumn {
        SectionHeading("Dynamic hierarchy")
        StructuralToggleCard()
        VisibleContrastCard()
    }
}

/**
 * The toggled `BorderPanel { ... }` is guarded by an `if`, so toggling the CheckBox composes or
 * disposes the whole subtree (a heading, an editable field, and a slider) rather than hiding it.
 * When unchecked, those nested controls and their state leave the tree entirely.
 */
@Composable
private fun StructuralToggleCard() {
    ExampleCard("Structural add/remove (conditional composition)") {
        var showDetails by remember { mutableStateOf(false) }
        CheckBox(
            text = "Show details panel",
            checked = showDetails,
            onCheckedChange = { showDetails = it },
        )
        WrappedCaption("The panel below is part of the tree only while the box is checked.")

        if (showDetails) {
            var name by remember { mutableStateOf("Ada") }
            var level by remember { mutableIntStateOf(3) }
            BorderPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED), hgap = 8, vgap = 8) {
                north {
                    Label("Details (this entire subtree was just inserted)")
                }
                center {
                    FlowPanel {
                        Label("Name:")
                        TextField(value = name, onValueChange = { name = it }, columns = 16)
                    }
                }
                south {
                    FlowPanel {
                        Label("Level: $level")
                        Slider(value = level, onValueChange = { level = it }, min = 0, max = 10)
                    }
                }
            }
        }
    }
}

/**
 * The counterpart to the structural toggle: the same panel always stays composed, and `visible()`
 * only flips its visibility. The subtree — and any state it holds — survives across toggles.
 */
@Composable
private fun VisibleContrastCard() {
    ExampleCard("Contrast: visible() keeps the slot") {
        var shown by remember { mutableStateOf(true) }
        CheckBox(
            text = "Show via visible() modifier",
            checked = shown,
            onCheckedChange = { shown = it },
        )
        WrappedCaption(
            "Structural if{}: the subtree is added/removed from the tree, taking its state with it. " +
                "visible(false): the component stays in the tree and only becomes hidden.",
        )

        var clicks by remember { mutableIntStateOf(0) }
        BorderPanel(modifier = SwingModifier.visible(shown).alignmentX(LEFT_ALIGNED), hgap = 8, vgap = 8) {
            north {
                Label("This panel is always composed; only its visibility changes.")
            }
            center {
                FlowPanel {
                    Button("Clicked $clicks time(s)", onClick = { clicks++ })
                }
            }
        }
    }
}
