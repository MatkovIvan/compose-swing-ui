@file:JvmMultifileClass
@file:JvmName("TextComponentsKt")

package org.jetbrains.compose.swing.components.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.components.documentChangeListener
import org.jetbrains.compose.swing.components.fullText
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.documentListener
import javax.swing.JTextArea
import javax.swing.event.DocumentListener

/**
 * A composable wrapper for JTextArea.
 *
 * For incremental editing over a shared `Document`, undo/redo, or observing the text as a flow, drive the
 * area with the [DocumentState] overload ([TextArea]) and a [DocumentState] from `rememberDocumentState`.
 *
 * @param value the current text value
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param onValueChange callback invoked when the text changes
 * @param rows the number of rows
 * @param columns the number of columns
 * @see TextArea the [DocumentState]-driven overload for large or complex editors
 */
@Composable
public fun TextArea(
    value: String,
    modifier: SwingModifier = SwingModifier,
    onValueChange: (String) -> Unit = {},
    rows: Int = 0,
    columns: Int = 0,
) {
    val callback = rememberUpdatedState(onValueChange)
    val listener =
        remember { documentChangeListener { event -> callback.value(event.document.fullText()) } }
    TextArea(value = value, documentListener = listener, modifier = modifier, rows = rows, columns = columns)
}

/**
 * A composable wrapper for JTextArea driven by a raw [DocumentListener] instead of an `onValueChange`
 * lambda. The [documentListener] is attached to the area's document as-is and removed on the same
 * instance; pass a stable instance (e.g. `remember {}`) to avoid churn.
 *
 * For incremental editing over a shared `Document`, undo/redo, or observing the text as a flow, drive the
 * area with the [DocumentState] overload ([TextArea]) and a [DocumentState] from `rememberDocumentState`.
 *
 * @param value the current text value
 * @param documentListener the listener notified of document edits
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param rows the number of rows
 * @param columns the number of columns
 * @see TextArea the [DocumentState]-driven overload for large or complex editors
 */
@Composable
public fun TextArea(
    value: String,
    documentListener: DocumentListener,
    modifier: SwingModifier = SwingModifier,
    rows: Int = 0,
    columns: Int = 0,
) {
    SwingNode(
        factory = { JTextArea(rows, columns) },
        update = {
            set(value) { setTextPreservingCaret(it) }
            applyModifier(SwingModifier.documentListener(documentListener) then modifier)
        },
    )
}

/**
 * A composable wrapper for JTextArea driven by a [DocumentState]. The area renders the state's own
 * document, so text typed into the area and edits made through the state are the same content, and the
 * caret is kept two-way with [DocumentState.selection]. The state is the single source of truth;
 * there is no `onValueChange`.
 *
 * @param state the hoistable text state the area renders and drives.
 * @param modifier the [SwingModifier] applied to the underlying component.
 * @param rows the number of rows.
 * @param columns the number of columns.
 */
@Composable
public fun TextArea(
    state: DocumentState,
    modifier: SwingModifier = SwingModifier,
    rows: Int = 0,
    columns: Int = 0,
) {
    SwingNode(
        factory = { JTextArea(rows, columns) },
        update = {
            applyModifier(documentStateBinding(state) then modifier)
        },
    )
}
