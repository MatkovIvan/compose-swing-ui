package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.jetbrains.compose.swing.components.CheckBoxMenuItem
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.Menu
import org.jetbrains.compose.swing.components.MenuItem
import org.jetbrains.compose.swing.components.MenuSeparator
import org.jetbrains.compose.swing.components.RadioButtonMenuItem
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.components.layout.ScrollPane
import org.jetbrains.compose.swing.components.selection.ListBox
import org.jetbrains.compose.swing.dialogs.FileChooserResult
import org.jetbrains.compose.swing.dialogs.showOpenDialog
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.accessibility.accessibleName
import org.jetbrains.compose.swing.modifier.appearance.border
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import java.awt.Dimension
import java.awt.Window
import javax.swing.BorderFactory
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

// The composable menu bar: File, Edit, and a View menu exercising CheckBoxMenuItem and a
// RadioButtonMenuItem group, all driven by hoisted state. File > New and File > Open drive the
// gallery's shared editor document, which the Editor section renders. The owner window is passed in
// because the menu bar is its own composition and does not receive the shell's LocalWindow; it anchors
// the file-chooser dialog.
@Composable
internal fun ShowcaseMenuBar(
    owner: Window,
    onExit: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var wrapText by remember { mutableStateOf(true) }
    var density by remember { mutableIntStateOf(0) }
    Menu("File") {
        MenuItem("New", onClick = { setEditorText("") })
        MenuItem("Open") {
            scope.launch {
                val result = showOpenDialog(parent = owner, title = "Open a file")
                // Cancelling leaves the current document untouched.
                if (result is FileChooserResult.Approved) setEditorText(result.file.readText())
            }
        }
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

// The sidebar + detail shell: the sidebar selection chooses which section body fills the center.
@Composable
internal fun ShowcaseShell() {
    var selected by remember { mutableIntStateOf(0) }
    BorderPanel(
        modifier = SwingModifier.border(BorderFactory.createEmptyBorder(12, 12, 12, 12)),
    ) {
        west {
            ScrollPane(modifier = SwingModifier.preferredSize(Dimension(180, 0))) {
                content {
                    ListBox(
                        items = showcaseSections.map { it.title },
                        selectedIndices = listOf(selected),
                        onSelectionChange = { indices -> indices.firstOrNull()?.let { selected = it } },
                        selectionMode = ListSelectionModel.SINGLE_SELECTION,
                        visibleRowCount = showcaseSections.size,
                        modifier = SwingModifier.accessibleName("Sections"),
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
