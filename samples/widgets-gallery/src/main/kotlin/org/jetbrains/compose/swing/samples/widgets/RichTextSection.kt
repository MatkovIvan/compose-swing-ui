package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.button.ToggleButton
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.components.layout.ScrollPane
import org.jetbrains.compose.swing.components.text.EditorPane
import org.jetbrains.compose.swing.components.text.TextPane
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.alignmentX
import org.jetbrains.compose.swing.modifier.preferredSize
import java.awt.Dimension

/**
 * Demonstrates the rich-text editors [EditorPane] and [TextPane], each bound to a
 * `remember { mutableStateOf(...) }` value so edits and the echo label stay in lock-step.
 */
@Composable
internal fun RichTextSection() {
    SectionColumn {
        SectionHeading("Rich text")
        EditorPaneCard()
        TextPaneCard()
    }
}

/**
 * EditorPane bound to one value; a ToggleButton flips [ContentType] between plain text and HTML so the
 * same markup is shown raw, then rendered. A CheckBox drives the editable flag.
 */
@Composable
private fun EditorPaneCard() {
    ExampleCard("EditorPane (PlainText / Html)") {
        var markup by remember { mutableStateOf("<h2>Hello</h2><p>Edit me, then render as <b>HTML</b>.</p>") }
        var html by remember { mutableStateOf(false) }
        var editable by remember { mutableStateOf(true) }

        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            ToggleButton(
                text = if (html) "Content type: Html" else "Content type: PlainText",
                pressed = html,
                onPressedChange = { html = it },
            )
            CheckBox(text = "Editable", checked = editable, onCheckedChange = { editable = it })
        }
        ScrollPane(modifier = SwingModifier.preferredSize(Dimension(360, 120)).alignmentX(LEFT_ALIGNED)) {
            content {
                EditorPane(
                    value = markup,
                    contentType = if (html) "text/html" else "text/plain",
                    onValueChange = { markup = it },
                    editable = editable,
                )
            }
        }
        Label("Rendered as ${if (html) "HTML" else "plain text"}")
    }
}

/** TextPane bound to a value; the echo label reports its current length as the user types. */
@Composable
private fun TextPaneCard() {
    ExampleCard("TextPane") {
        var notes by remember { mutableStateOf("A styled-document editor.\nType here.") }
        var editable by remember { mutableStateOf(true) }

        CheckBox(text = "Editable", checked = editable, onCheckedChange = { editable = it })
        ScrollPane(modifier = SwingModifier.preferredSize(Dimension(360, 100)).alignmentX(LEFT_ALIGNED)) {
            content {
                TextPane(
                    value = notes,
                    onValueChange = { notes = it },
                    editable = editable,
                )
            }
        }
        Label("Length: ${notes.length}")
    }
}
