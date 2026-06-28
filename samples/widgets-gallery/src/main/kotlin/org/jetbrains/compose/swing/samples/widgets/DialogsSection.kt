package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.dialogs.FileChooserResult
import org.jetbrains.compose.swing.dialogs.showColorDialog
import org.jetbrains.compose.swing.dialogs.showConfirmDialog
import org.jetbrains.compose.swing.dialogs.showInputDialog
import org.jetbrains.compose.swing.dialogs.showMessageDialog
import org.jetbrains.compose.swing.dialogs.showOpenDialog
import org.jetbrains.compose.swing.window.LocalWindow
import javax.swing.JOptionPane

/**
 * Demonstrates the standard dialog helpers — message, confirm, input, file chooser, and colour chooser.
 * Each button launches a coroutine from [rememberCoroutineScope] that calls a suspend dialog function,
 * anchoring it to the owning [LocalWindow], then writes the awaited result into a Label.
 */
@Composable
internal fun DialogsSection() {
    SectionColumn {
        SectionHeading("Standard dialogs")
        MessageDialogCard()
        ConfirmDialogCard()
        InputDialogCard()
        FileChooserCard()
        ColorChooserCard()
    }
}

/** A plain message dialog; the Label confirms once the user dismisses it. */
@Composable
private fun MessageDialogCard() {
    ExampleCard("Message dialog") {
        val owner = LocalWindow.current
        val scope = rememberCoroutineScope()
        var status by remember { mutableStateOf("Not shown yet") }
        FlowPanel {
            Button("Show message") {
                scope.launch {
                    showMessageDialog("Saved successfully.", parent = owner, title = "Message")
                    status = "Message acknowledged"
                }
            }
        }
        Label("Status: $status")
    }
}

/** A Yes/No/Cancel confirm dialog; the Label echoes which button was chosen. */
@Composable
private fun ConfirmDialogCard() {
    ExampleCard("Confirm dialog") {
        val owner = LocalWindow.current
        val scope = rememberCoroutineScope()
        var answer by remember { mutableStateOf("No answer yet") }
        FlowPanel {
            Button("Ask to proceed") {
                scope.launch {
                    val result =
                        showConfirmDialog(
                            "Apply these changes?",
                            parent = owner,
                            title = "Confirm",
                            option = JOptionPane.YES_NO_CANCEL_OPTION,
                        )
                    answer = "Answer: $result"
                }
            }
        }
        Label(answer)
    }
}

/** A text-input dialog; the Label shows the entered text, or notes a cancellation. */
@Composable
private fun InputDialogCard() {
    ExampleCard("Input dialog") {
        val owner = LocalWindow.current
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("Nothing entered") }
        FlowPanel {
            Button("Enter your name") {
                scope.launch {
                    val entered =
                        showInputDialog(
                            "What is your name?",
                            parent = owner,
                            title = "Input",
                            initialValue = "Anonymous",
                        )
                    name = entered?.let { "Entered: $it" } ?: "Cancelled"
                }
            }
        }
        Label(name)
    }
}

/** A JFileChooser open dialog; the Label reports the chosen path or a cancellation. */
@Composable
private fun FileChooserCard() {
    ExampleCard("File chooser") {
        val owner = LocalWindow.current
        val scope = rememberCoroutineScope()
        var selection by remember { mutableStateOf("No file chosen") }
        FlowPanel {
            Button("Choose a file") {
                scope.launch {
                    selection =
                        when (val result = showOpenDialog(parent = owner, title = "Open a file")) {
                            is FileChooserResult.Approved -> "Chosen: ${result.file.absolutePath}"
                            FileChooserResult.Cancelled -> "Cancelled"
                        }
                }
            }
        }
        Label(selection)
    }
}

/** A JColorChooser dialog; the Label reports the chosen color's RGB, or a cancellation. */
@Composable
private fun ColorChooserCard() {
    ExampleCard("Color chooser") {
        val owner = LocalWindow.current
        val scope = rememberCoroutineScope()
        var color by remember { mutableStateOf("No color chosen") }
        FlowPanel {
            Button("Pick a color") {
                scope.launch {
                    val chosen = showColorDialog(parent = owner, title = "Pick a color")
                    color =
                        chosen?.let { "RGB: ${it.red}, ${it.green}, ${it.blue}" } ?: "Cancelled"
                }
            }
        }
        Label(color)
    }
}
