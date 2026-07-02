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
import javax.swing.text.Document
import javax.swing.text.EditorKit
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit

/**
 * A composable wrapper for `JEditorPane`.
 *
 * The pane interprets [value] according to [contentType]: as plain text, HTML, or RTF. Changing
 * [contentType] reinterprets [value] under the new type. The binding is reactive in both directions —
 * [value] is pushed onto the pane, and edits the user makes are reported through [onValueChange].
 *
 * For incremental editing over a shared `Document`, undo/redo, or observing the text as a flow, drive the
 * pane with the [DocumentState] overload ([EditorPane]) and a [DocumentState] from `rememberDocumentState`.
 *
 * @param value the current text, interpreted as [contentType]
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param contentType the MIME type the text is interpreted as (a [ContentType] MIME string)
 * @param onValueChange callback invoked when the text changes
 * @param editable whether the user can edit the text
 * @see EditorPane the [DocumentState]-driven overload for large or complex editors
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
 * For incremental editing over a shared `Document`, undo/redo, or observing the text as a flow, drive the
 * pane with the [DocumentState] overload ([EditorPane]) and a [DocumentState] from `rememberDocumentState`.
 *
 * @param value the current text, interpreted as [contentType]
 * @param documentListener the listener notified of document edits
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param contentType the MIME type the text is interpreted as (a [ContentType] MIME string)
 * @param editable whether the user can edit the text
 * @see EditorPane the [DocumentState]-driven overload for large or complex editors
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
 * A composable wrapper for `JEditorPane` driven by a [DocumentState]. The pane renders the state's
 * own document, so text typed into the pane and edits made through the state are the same content, and
 * the caret is kept two-way with [DocumentState.selection]. The state is the single source of
 * truth; there is no `onValueChange`.
 *
 * The pane's content type follows the state's document: a `PlainDocument` (the default from
 * `rememberDocumentState`) renders as plain text, and an `HTMLDocument` renders as HTML. To render HTML,
 * back the state with a matching document, for example
 * `rememberDocumentState(document = HTMLEditorKit().createDefaultDocument())`. The document type and the
 * rendered content type are one thing, so they can never disagree.
 *
 * @param state the hoistable text state the pane renders and drives.
 * @param modifier the [SwingModifier] applied to the underlying component.
 * @param editable whether the user can edit the text.
 */
@Composable
public fun EditorPane(
    state: DocumentState,
    modifier: SwingModifier = SwingModifier,
    editable: Boolean = true,
) {
    SwingNode(
        factory = { JEditorPane() },
        update = {
            // Install the editor kit that renders the state's document before binding it: JEditorPane
            // couples the rendered content type to the kit, and each kit expects its own document type,
            // so an HTMLDocument needs the HTMLEditorKit whose views can read it. A plain document keeps
            // the pane's factory-default kit, which already provides plain-text views.
            set(state) {
                htmlEditorKitFor(state.document)?.let { kit -> editorKit = kit }
                state.bind(this)
            }
            set(editable) { this.isEditable = it }
            applyModifier(modifier)
        },
        onRelease = { state.unbind(this) },
    )
}

// The HTML editor kit an HTMLDocument needs to render, or null for any other document, which the pane's
// default plain-text kit already handles. Installing the kit switches the pane's reported content type to
// "text/html", so the document type and the rendered content type stay one and the same.
private fun htmlEditorKitFor(document: Document): EditorKit? = if (document is HTMLDocument) HTMLEditorKit() else null

/**
 * A composable wrapper for `JTextPane`, an editor over a styled document.
 *
 * The binding is reactive in both directions — [value] is pushed onto the pane, and edits the user
 * makes are reported through [onValueChange].
 *
 * For incremental editing over a shared `Document`, undo/redo, or observing the text as a flow, drive the
 * pane with the [DocumentState] overload ([TextPane]) and a [DocumentState] from `rememberDocumentState`.
 *
 * @param value the current text
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param onValueChange callback invoked when the text changes
 * @param editable whether the user can edit the text
 * @see TextPane the [DocumentState]-driven overload for large or complex editors
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
 * For incremental editing over a shared `Document`, undo/redo, or observing the text as a flow, drive the
 * pane with the [DocumentState] overload ([TextPane]) and a [DocumentState] from `rememberDocumentState`.
 *
 * @param value the current text
 * @param documentListener the listener notified of document edits
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param editable whether the user can edit the text
 * @see TextPane the [DocumentState]-driven overload for large or complex editors
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

/**
 * A composable wrapper for `JTextPane` driven by a [DocumentState]. The pane renders the state's
 * own document, so text typed into the pane and edits made through the state are the same content, and
 * the caret is kept two-way with [DocumentState.selection]. The state is the single source of
 * truth; there is no `onValueChange`.
 *
 * A `JTextPane` renders a styled document, so [state] must wrap a `StyledDocument` (e.g. a
 * `DefaultStyledDocument` passed to `rememberDocumentState(document = …)`).
 *
 * @param state the hoistable text state, over a `StyledDocument`, the pane renders and drives.
 * @param modifier the [SwingModifier] applied to the underlying component.
 * @param editable whether the user can edit the text.
 */
@Composable
public fun TextPane(
    state: DocumentState,
    modifier: SwingModifier = SwingModifier,
    editable: Boolean = true,
) {
    SwingNode(
        factory = { JTextPane() },
        update = {
            set(state) { state.bind(this) }
            set(editable) { this.isEditable = it }
            applyModifier(modifier)
        },
        onRelease = { state.unbind(this) },
    )
}
