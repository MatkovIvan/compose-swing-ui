package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.swing.components.ComboBox
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.foreground
import java.awt.Color

/** The accent colour flowing down the composition; consumed several layers deep to style labels. */
private val LocalAccent = staticCompositionLocalOf { Color.BLUE }

/**
 * Demonstrates Compose [staticCompositionLocalOf] driving Swing components. A picker stores the
 * chosen accent in state and provides it through [CompositionLocalProvider]; nested helpers read
 * `LocalAccent.current` to colour their labels, so changing the selection repaints the whole subtree.
 */
@Composable
internal fun CompositionLocalsSection() {
    SectionColumn {
        SectionHeading("Composition locals")
        ExampleCard("CompositionLocal over Swing") {
            val choices =
                listOf(
                    "Blue" to Color(0x42, 0x85, 0xF4),
                    "Green" to Color(0x2E, 0x7D, 0x32),
                    "Crimson" to Color(0xC6, 0x28, 0x28),
                    "Purple" to Color(0x6A, 0x1B, 0x9A),
                )
            var accentIndex by remember { mutableIntStateOf(0) }
            FlowPanel {
                Label("Accent:")
                ComboBox(
                    items = choices.map { it.first },
                    selectedIndex = accentIndex,
                    onSelectionChange = { accentIndex = it },
                )
            }
            // Provide the chosen accent once, near the top; everything below reads it implicitly.
            CompositionLocalProvider(LocalAccent provides choices[accentIndex].second) {
                OuterPanel()
            }
        }
    }
}

/** First nesting level: forwards the subtree without ever touching the local. */
@Composable
private fun OuterPanel() {
    FlowPanel {
        Label("Outer level (does not read the local)")
    }
    MiddlePanel()
}

/** Second nesting level: again only structural, proving the value flows past unaware ancestors. */
@Composable
private fun MiddlePanel() {
    FlowPanel {
        AccentedLabel("Middle-level accented label")
    }
    InnerPanel()
}

/** Third nesting level: the deepest consumer, several layers from where the local was provided. */
@Composable
private fun InnerPanel() {
    FlowPanel {
        AccentedLabel("Inner-level accented label")
    }
}

/** Leaf consumer: reads `LocalAccent.current` and styles its [Label] with it. */
@Composable
private fun AccentedLabel(text: String) {
    val accent = LocalAccent.current
    Label(text = text, modifier = SwingModifier.foreground(accent))
}
