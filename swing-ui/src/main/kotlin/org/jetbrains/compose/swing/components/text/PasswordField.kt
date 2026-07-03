@file:JvmMultifileClass
@file:JvmName("TextComponentsKt")

package org.jetbrains.compose.swing.components.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.SwingNodeUpdater
import org.jetbrains.compose.swing.components.documentChangeListener
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.documentListener
import javax.swing.JPasswordField
import javax.swing.event.DocumentListener
import javax.swing.text.Document
import javax.swing.text.Segment

/**
 * A composable wrapper for JPasswordField.
 *
 * The value is a [CharArray] of raw characters rather than a `String`, so a security-sensitive caller
 * controls every copy of the password.
 *
 * Array ownership: the array delivered to [onValueChange] is a fresh copy owned by the receiver,
 * free to retain or zero. The [value] array stays owned by the caller, read only through the next
 * recomposition; zeroing it once it stops being the current value is the caller's responsibility.
 *
 * For incremental editing over a shared `Document`, undo/redo, or observing the text as a flow, drive the
 * field with [PasswordField] and a [DocumentState] from `rememberDocumentState`.
 *
 * @param value the current text value, as raw characters
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param onValueChange callback invoked when the text changes, with the typed characters
 * @param echoChar the masking character; `null` applies the look-and-feel's installed echo character,
 *   and the NUL character (U+0000) shows the text in clear text
 * @param columns the number of columns
 * @see PasswordField the [DocumentState]-driven overload for large or complex editors
 */
@Composable
public fun PasswordField(
    value: CharArray,
    modifier: SwingModifier = SwingModifier,
    onValueChange: (CharArray) -> Unit = {},
    echoChar: Char? = null,
    columns: Int = 0,
) {
    val callback = rememberUpdatedState(onValueChange)
    // Deliver the raw characters by reading the document into a char array via a Segment, keeping
    // the password out of an unzeroable String.
    val listener =
        remember { documentChangeListener { event -> callback.value(event.document.fullPassword()) } }
    PasswordField(
        value = value,
        documentListener = listener,
        modifier = modifier,
        echoChar = echoChar,
        columns = columns,
    )
}

/**
 * A composable wrapper for JPasswordField driven by a raw [DocumentListener] instead of an
 * `onValueChange` lambda. The [documentListener] is attached to the field's document as-is and removed
 * on the same instance; pass a stable instance (e.g. `remember {}`) to avoid churn.
 *
 * The [value] array stays owned by the caller, read only through the next recomposition; zeroing
 * it once it stops being the current value is the caller's responsibility.
 *
 * For incremental editing over a shared `Document`, undo/redo, or observing the text as a flow, drive the
 * field with [PasswordField] and a [DocumentState] from `rememberDocumentState`.
 *
 * @param value the current text value, as raw characters
 * @param documentListener the listener notified of document edits
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param echoChar the masking character; `null` applies the look-and-feel's installed echo character,
 *   and the NUL character (U+0000) shows the text in clear text
 * @param columns the number of columns
 * @see PasswordField the [DocumentState]-driven overload for large or complex editors
 */
@Composable
public fun PasswordField(
    value: CharArray,
    documentListener: DocumentListener,
    modifier: SwingModifier = SwingModifier,
    echoChar: Char? = null,
    columns: Int = 0,
) {
    PasswordFieldImpl(
        echoChar = echoChar,
        columns = columns,
        update = {
            // CharArray has identity equality, so `set(value)` runs on every recomposition; the
            // content compare against the live getPassword() is what actually guards the write and
            // prevents resetting the caret when the field already holds these characters.
            set(value) { if (!this.password.contentEquals(it)) this.text = String(it) }
            applyModifier(SwingModifier.documentListener(documentListener) then modifier)
        },
    )
}

/**
 * A composable wrapper for JPasswordField driven by a [DocumentState]. The field renders the
 * state's own document, so masked text typed into the field and edits made through the state are the
 * same content, and the caret is kept two-way with [DocumentState.selection]. The state is the
 * single source of truth.
 *
 * The state models plain text: [DocumentState.text] materializes the password as an ordinary
 * `String`, which the caller cannot zero and which persists on the heap until garbage-collected.
 *
 * @param state the hoistable text state the field renders and drives.
 * @param modifier the [SwingModifier] applied to the underlying component.
 * @param echoChar the masking character; `null` applies the look-and-feel's installed echo character,
 *   and the NUL character (U+0000) shows the text in clear text.
 * @param columns the number of columns.
 */
@Composable
public fun PasswordField(
    state: DocumentState,
    modifier: SwingModifier = SwingModifier,
    echoChar: Char? = null,
    columns: Int = 0,
) {
    PasswordFieldImpl(
        echoChar = echoChar,
        columns = columns,
        update = {
            applyModifier(documentStateBinding(state) then modifier)
        },
    )
}

/**
 * Shared scaffolding for the [PasswordField] overloads: constructs the field with [columns], keeps
 * [echoChar] applied, and threads each overload's own binding through [update].
 *
 * The look-and-feel installs a default echo character on a freshly constructed field; capture it so that
 * re-applying a null [echoChar] reverts to that default rather than leaving a stale custom mask.
 */
@Composable
private fun PasswordFieldImpl(
    echoChar: Char?,
    columns: Int,
    update: SwingNodeUpdater<JPasswordField>.() -> Unit,
) {
    val defaultEchoChar = remember { CharArray(1) }
    SwingNode(
        factory = { JPasswordField(columns).also { defaultEchoChar[0] = it.echoChar } },
        update = {
            set(echoChar) { this.echoChar = it ?: defaultEchoChar[0] }
            update()
        },
    )
}

/**
 * Reads the full text of the receiver [Document] into a fresh [CharArray] via a [Segment], keeping
 * the password out of an unzeroable `String`, so a security-sensitive caller can zero the returned
 * array after use.
 */
private fun Document.fullPassword(): CharArray {
    val segment = Segment().apply { isPartialReturn = false }
    getText(0, length, segment)
    return segment.array.copyOfRange(segment.offset, segment.offset + segment.count)
}
