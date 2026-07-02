package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.CheckBoxMenuItem
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.Menu
import org.jetbrains.compose.swing.components.MenuItem
import org.jetbrains.compose.swing.components.MenuSeparator
import org.jetbrains.compose.swing.components.RadioButtonMenuItem
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.components.layout.ScrollPane
import org.jetbrains.compose.swing.components.selection.ListBox
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.border
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

private const val BODY_INSET = 12

/**
 * The composable menu bar: a File menu, an Edit menu, and a View menu exercising
 * [CheckBoxMenuItem] and a [RadioButtonMenuItem] group, all driven by hoisted state.
 */
@Composable
internal fun ShowcaseMenuBar(onExit: () -> Unit) {
    var wrapText by remember { mutableStateOf(true) }
    var density by remember { mutableIntStateOf(0) }
    Menu("File") {
        MenuItem("New")
        MenuItem("Open")
        MenuSeparator()
        MenuItem("Exit", onClick = onExit)
    }
    Menu("Edit") {
        MenuItem("Cut")
        MenuItem("Copy")
        MenuItem("Paste")
    }
    Menu("View") {
        CheckBoxMenuItem("Wrap text", checked = wrapText, onCheckedChange = { wrapText = it })
        MenuSeparator()
        RadioButtonMenuItem("Comfortable", selected = density == 0, onSelect = { density = 0 })
        RadioButtonMenuItem("Compact", selected = density == 1, onSelect = { density = 1 })
    }
}

/**
 * The sidebar + detail shell. The sidebar selection chooses which section body fills the center. A
 * uniform body inset wraps the whole shell so the content breathes inside the window.
 */
@Composable
internal fun ShowcaseShell() {
    var selected by remember { mutableIntStateOf(0) }
    BorderPanel(
        modifier =
            SwingModifier
                .testTag(SHOWCASE_SHELL_TAG)
                .border(BorderFactory.createEmptyBorder(BODY_INSET, BODY_INSET, BODY_INSET, BODY_INSET)),
    ) {
        west {
            ScrollPane(modifier = SwingModifier.preferredSize(Dimension(SIDEBAR_WIDTH, 0))) {
                content {
                    ListBox(
                        items = showcaseSections.map { it.title },
                        selectedIndices = listOf(selected),
                        onSelectionChange = { indices -> indices.firstOrNull()?.let { selected = it } },
                        selectionMode = ListSelectionModel.SINGLE_SELECTION,
                        visibleRowCount = showcaseSections.size,
                        modifier = SwingModifier.testTag(SECTION_LIST_TAG),
                    )
                }
            }
        }
        center {
            showcaseSections.getOrNull(selected)?.body?.invoke()
        }
        south {
            Label(
                "Section: ${showcaseSections.getOrNull(selected)?.title ?: "—"}",
                horizontalAlignment = SwingConstants.LEADING,
            )
        }
    }
}

/** Test tag for the showcase shell's root, used by the behavioral tests to scope queries. */
internal const val SHOWCASE_SHELL_TAG: String = "showcase-shell"

/** Test tag for the section-selector list, used by the behavioral tests to switch sections. */
internal const val SECTION_LIST_TAG: String = "showcase-section-list"

private const val SIDEBAR_WIDTH = 180
