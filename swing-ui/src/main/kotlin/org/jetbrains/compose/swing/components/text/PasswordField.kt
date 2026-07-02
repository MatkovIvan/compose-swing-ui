@file:JvmMultifileClass
@file:JvmName("TextComponentsKt")

package org.jetbrains.compose.swing.components.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
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
 * @param value the current text value, as raw characters
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param onValueChange callback invoked when the text changes, with the typed characters
 * @param echoChar the masking character; `null` applies the look-and-feel's installed echo character,
 *   and the NUL character (U+0000) shows the text in clear text
 * @param columns the number of columns
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
 * @param value the current text value, as raw characters
 * @param documentListener the listener notified of document edits
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param echoChar the masking character; `null` applies the look-and-feel's installed echo character,
 *   and the NUL character (U+0000) shows the text in clear text
 * @param columns the number of columns
 */
@Composable
public fun PasswordField(
    value: CharArray,
    documentListener: DocumentListener,
    modifier: SwingModifier = SwingModifier,
    echoChar: Char? = null,
    columns: Int = 0,
) {
    // The look-and-feel installs a default echo character on a freshly constructed field; capture it so
    // that re-applying a null echoChar reverts to that default rather than leaving a stale custom mask.
    val defaultEchoChar = remember { CharArray(1) }
    SwingNode(
        factory = { JPasswordField(columns).also { defaultEchoChar[0] = it.echoChar } },
        update = {
            // CharArray has identity equality, so `set(value)` runs on every recomposition; the
            // content compare against the live getPassword() is what actually guards the write and
            // prevents resetting the caret when the field already holds these characters.
            set(value) { if (!this.password.contentEquals(it)) this.text = String(it) }
            set(echoChar) { this.echoChar = it ?: defaultEchoChar[0] }
            applyModifier(SwingModifier.documentListener(documentListener) then modifier)
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
