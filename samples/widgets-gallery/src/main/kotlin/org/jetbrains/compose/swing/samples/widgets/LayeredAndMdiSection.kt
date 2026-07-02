package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.desktop.DesktopPane
import org.jetbrains.compose.swing.components.desktop.InternalFrameControls
import org.jetbrains.compose.swing.components.desktop.LayeredPane
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.background
import org.jetbrains.compose.swing.modifier.appearance.opaque
import org.jetbrains.compose.swing.modifier.layout.bounds
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import java.awt.Color
import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JLayeredPane

/**
 * Demonstrates the layered/MDI surfaces: [LayeredPane] stacks self-positioned children on depth
 * layers, and [DesktopPane] floats internal frames whose controlled close routes through `onClose`.
 */
@Composable
internal fun LayeredAndMdiSection() {
    SectionColumn {
        SectionHeading("Layered & MDI")
        LayeredPaneCard()
        DesktopPaneCard()
    }
}

/**
 * Three children stacked on increasing depth layers. A layered pane does not lay its children out, so
 * each child positions itself with `bounds(...)`; the overlapping tints make the paint order visible —
 * higher layers cover lower ones.
 */
@Composable
private fun LayeredPaneCard() {
    ExampleCard("LayeredPane (depth layers)") {
        LayeredPane(modifier = SwingModifier.preferredSize(Dimension(240, 160))) {
            layer(JLayeredPane.DEFAULT_LAYER) {
                BorderPanel(
                    modifier =
                        SwingModifier
                            .bounds(10, 10, 160, 110)
                            .opaque(true)
                            .background(Color(0xBBDEFB)),
                ) {
                    north { Label("Default layer") }
                }
            }
            layer(JLayeredPane.PALETTE_LAYER) {
                BorderPanel(
                    modifier =
                        SwingModifier
                            .bounds(60, 40, 140, 90)
                            .opaque(true)
                            .background(Color(0xFFE0B2)),
                ) {
                    north { Label("Palette layer") }
                }
            }
            layer(JLayeredPane.DRAG_LAYER) {
                BorderPanel(
                    modifier =
                        SwingModifier
                            .bounds(110, 70, 110, 70)
                            .opaque(true)
                            .background(Color(0xC8E6C9)),
                ) {
                    north { Label("Drag layer (top)") }
                }
            }
        }
    }
}

/**
 * Two floating internal frames plus a state-driven third one. The "Add"/"Remove" button adds or drops
 * the extra frame's declaration; its close control is controlled, so closing it routes through `onClose`
 * which clears the same state — closing and the button stay in sync.
 */
@Composable
private fun DesktopPaneCard() {
    ExampleCard("DesktopPane (internal frames)") {
        var extraOpen by remember { mutableStateOf(false) }
        var closedCount by remember { mutableIntStateOf(0) }

        FlowPanel {
            Button(
                text = if (extraOpen) "Remove frame" else "Add frame",
                onClick = { extraOpen = !extraOpen },
            )
            Label("Controlled closes: $closedCount")
        }

        DesktopPane(modifier = SwingModifier.preferredSize(Dimension(320, 220))) {
            internalFrame(title = "Editor", bounds = Rectangle(0, 0, 180, 120)) {
                Label("Editor frame")
            }
            internalFrame(title = "Console", bounds = Rectangle(60, 50, 180, 120)) {
                Label("Console frame")
            }
            if (extraOpen) {
                internalFrame(
                    title = "Inspector",
                    bounds = Rectangle(120, 90, 180, 110),
                    controls = InternalFrameControls(closable = true),
                    onClose = {
                        closedCount++
                        extraOpen = false
                    },
                ) {
                    Label("Inspector frame")
                }
            }
        }
    }
}
