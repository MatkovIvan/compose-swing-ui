package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.components.layout.Panel
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.border
import org.jetbrains.compose.swing.modifier.datatransfer.copyToClipboard
import org.jetbrains.compose.swing.modifier.datatransfer.draggable
import org.jetbrains.compose.swing.modifier.datatransfer.dropTarget
import org.jetbrains.compose.swing.modifier.datatransfer.pasteFromClipboard
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import org.jetbrains.compose.swing.modifier.listener.hierarchyListener
import java.awt.Color
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.HierarchyEvent
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.TransferHandler

// The data-transfer modifiers: drag-and-drop between components and system-clipboard copy/paste.
// All payloads are plain strings carried as a StringSelection.
@Composable
internal fun DataTransferSection() {
    SectionColumn {
        SectionHeading("Data transfer")
        DragAndDropCard()
        ClipboardCard()
    }
}

@Composable
private fun DragAndDropCard() {
    ExampleCard("draggable + dropTarget (drag the label onto the panel)") {
        val payload = "Hello from the drag source"
        var dropped by remember { mutableStateOf("Nothing dropped yet") }

        FlowPanel {
            Label(
                text = payload,
                modifier =
                    SwingModifier
                        .border(BorderFactory.createDashedBorder(Color.GRAY))
                        .draggable(exportedActions = TransferHandler.COPY) { StringSelection(payload) },
            )
        }
        FlowPanel {
            Panel(
                modifier =
                    SwingModifier
                        .preferredSize(Dimension(240, 60))
                        .border(BorderFactory.createLineBorder(Color.GRAY))
                        .dropTarget(
                            acceptedActions = TransferHandler.COPY,
                            canImport = { flavors -> DataFlavor.stringFlavor in flavors },
                            onDrop = { transferable ->
                                dropped = transferable.getTransferData(DataFlavor.stringFlavor) as String
                                true
                            },
                        ),
            ) {
                Label("Drop here: $dropped")
            }
        }
    }
}

@Composable
private fun ClipboardCard() {
    ExampleCard("copyToClipboard + pasteFromClipboard (explicit buttons)") {
        var text by remember { mutableStateOf("Copy me to the system clipboard") }
        var status by remember { mutableStateOf("") }
        var field by remember { mutableStateOf<JComponent?>(null) }

        FlowPanel {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier =
                    SwingModifier.hierarchyListener { event ->
                        if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                            field = event.component as JComponent
                        }
                    },
            )
        }
        FlowPanel {
            Button("Copy") { field?.let { copyToClipboard(it) } }
            Button("Paste") {
                status = field?.let { if (pasteFromClipboard(it)) "Pasted" else "Nothing to paste" }.orEmpty()
            }
            Label(status)
        }
    }
}
