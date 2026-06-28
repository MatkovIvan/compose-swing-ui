package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.CheckBoxMenuItem
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.Menu
import org.jetbrains.compose.swing.components.MenuItem
import org.jetbrains.compose.swing.components.MenuSeparator
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.interaction.contextMenu

/**
 * Demonstrates [SwingModifier.contextMenu]: a right-click popup whose items are the same menu
 * composables used by a menu bar, with each selection updating hoisted Compose state that a status
 * label reflects.
 */
@Composable
internal fun ContextMenuSection() {
    SectionColumn {
        SectionHeading("Context menu")
        ActionMenuCard()
        StatefulMenuCard()
    }
}

/**
 * A label that opens a popup of plain actions on right-click. Selecting an item records the action
 * name into hoisted state, which the status label below echoes.
 */
@Composable
private fun ActionMenuCard() {
    ExampleCard("contextMenu with MenuItem actions") {
        var lastAction by remember { mutableStateOf("none") }
        val menu =
            SwingModifier.contextMenu {
                MenuItem("Cut") { lastAction = "Cut" }
                MenuItem("Copy") { lastAction = "Copy" }
                MenuItem("Paste") { lastAction = "Paste" }
                MenuSeparator()
                Menu("More") {
                    MenuItem("Select all") { lastAction = "Select all" }
                    MenuItem("Clear") { lastAction = "Clear" }
                }
            }
        Label("Right-click here for actions", modifier = menu)
        Label("Last action: $lastAction")
    }
}

/**
 * The popup reads live composition state, so its [CheckBoxMenuItem]s show the current flags and their
 * callbacks toggle hoisted state that the status label mirrors.
 */
@Composable
private fun StatefulMenuCard() {
    ExampleCard("contextMenu with CheckBoxMenuItem state") {
        var wrap by remember { mutableStateOf(true) }
        var lineNumbers by remember { mutableStateOf(false) }
        val menu =
            SwingModifier.contextMenu {
                CheckBoxMenuItem("Word wrap", checked = wrap) { wrap = it }
                CheckBoxMenuItem("Line numbers", checked = lineNumbers) { lineNumbers = it }
            }
        Label("Right-click here to toggle options", modifier = menu)
        Label("Word wrap: ${if (wrap) "on" else "off"}, line numbers: ${if (lineNumbers) "on" else "off"}")
    }
}
