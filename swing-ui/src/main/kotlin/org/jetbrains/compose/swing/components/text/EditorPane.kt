@file:JvmMultifileClass
@file:JvmName("TextComponentsKt")

package org.jetbrains.compose.swing.components.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.components.documentChangeListener
import org.jetbrains.compose.swing.components.fullText
import org.jetbrains.compose.swing.components.swappableDocumentListener
import org.jetbrains.compose.swing.constants.ContentType
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.documentListener
import javax.swing.JEditorPane
import javax.swing.JTextPane
import javax.swing.event.DocumentListener

/**
 * A composable wrapper for `JEditorPane`.
 *
 * The pane interprets [value] according to [contentType]: as plain text, HTML, or RTF. Changing
 * [contentType] reinterprets [value] under the new type. The binding is reactive in both directions —
 * [value] is pushed onto the pane, and edits the user makes are reported through [onValueChange].
 *
 * @param value the current text, interpreted as [contentType]
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param contentType the MIME type the text is interpreted as (a [ContentType] MIME string)
 * @param onValueChange callback invoked when the text changes
 * @param editable whether the user can edit the text
 */
@Composable
public fun EditorPane(
    value: String,
    modifier: SwingModifier = SwingModifier,
    @ContentType contentType: String = "text/plain",
    onValueChange: (String) -> Unit = {},
    editable: Boolean = true,
) {
    val callback = rememberUpdatedState(onValueChange)
    // A switch of content type installs a fresh document, so the binding follows the swap via
    // swappableDocumentListener; the listener reads the pane's text from the document that fired.
    val listener = remember { documentChangeListener { event -> callback.value(event.document.fullText()) } }
    EditorPaneImpl(
        value = value,
        modifier = modifier,
        contentType = contentType,
        editable = editable,
        installListener = { base -> base.swappableDocumentListener(listener) },
    )
}

/**
 * A [EditorPane] driven by a raw [DocumentListener] instead of an `onValueChange` lambda. The listener
 * observes the document the pane holds at install time; pass a stable instance (e.g. `remember {}`) to
 * avoid churn.
 *
 * @param value the current text, interpreted as [contentType]
 * @param documentListener the listener notified of document edits
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param contentType the MIME type the text is interpreted as (a [ContentType] MIME string)
 * @param editable whether the user can edit the text
 */
@Composable
public fun EditorPane(
    value: String,
    documentListener: DocumentListener,
    modifier: SwingModifier = SwingModifier,
    @ContentType contentType: String = "text/plain",
    editable: Boolean = true,
) {
    EditorPaneImpl(
        value = value,
        modifier = modifier,
        contentType = contentType,
        editable = editable,
        installListener = { base -> base.documentListener(documentListener) },
    )
}

@Composable
private fun EditorPaneImpl(
    value: String,
    modifier: SwingModifier,
    @ContentType contentType: String,
    editable: Boolean,
    installListener: (SwingModifier) -> SwingModifier,
) {
    SwingNode(
        factory = { JEditorPane() },
        update = {
            // Apply the content type before the text: switching type installs a fresh editor kit and
            // document, so the text must be (re)set against the document that interprets it.
            set(contentType) { this.contentType = it }
            set(value) { setTextPreservingCaret(it) }
            set(editable) { this.isEditable = it }
            applyModifier(installListener(SwingModifier) then modifier)
        },
    )
}

/**
 * A composable wrapper for `JTextPane`, an editor over a styled document.
 *
 * The binding is reactive in both directions — [value] is pushed onto the pane, and edits the user
 * makes are reported through [onValueChange].
 *
 * @param value the current text
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param onValueChange callback invoked when the text changes
 * @param editable whether the user can edit the text
 */
@Composable
public fun TextPane(
    value: String,
    modifier: SwingModifier = SwingModifier,
    onValueChange: (String) -> Unit = {},
    editable: Boolean = true,
) {
    val callback = rememberUpdatedState(onValueChange)
    val listener = remember { documentChangeListener { event -> callback.value(event.document.fullText()) } }
    TextPaneImpl(
        value = value,
        modifier = modifier,
        editable = editable,
        installListener = { base -> base.swappableDocumentListener(listener) },
    )
}

/**
 * A [TextPane] driven by a raw [DocumentListener] instead of an `onValueChange` lambda. The listener
 * observes the document the pane holds at install time; pass a stable instance (e.g. `remember {}`) to
 * avoid churn.
 *
 * @param value the current text
 * @param documentListener the listener notified of document edits
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param editable whether the user can edit the text
 */
@Composable
public fun TextPane(
    value: String,
    documentListener: DocumentListener,
    modifier: SwingModifier = SwingModifier,
    editable: Boolean = true,
) {
    TextPaneImpl(
        value = value,
        modifier = modifier,
        editable = editable,
        installListener = { base -> base.documentListener(documentListener) },
    )
}

@Composable
private fun TextPaneImpl(
    value: String,
    modifier: SwingModifier,
    editable: Boolean,
    installListener: (SwingModifier) -> SwingModifier,
) {
    SwingNode(
        factory = { JTextPane() },
        update = {
            set(value) { setTextPreservingCaret(it) }
            set(editable) { this.isEditable = it }
            applyModifier(installListener(SwingModifier) then modifier)
        },
    )
}
