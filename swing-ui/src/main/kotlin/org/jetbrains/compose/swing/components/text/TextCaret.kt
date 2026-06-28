@file:JvmMultifileClass
@file:JvmName("TextComponentsKt")

package org.jetbrains.compose.swing.components.text

import javax.swing.text.JTextComponent

/**
 * Sets the component's text only when it differs from the current text. Re-setting the same text resets
 * the caret to the document start, so this guard preserves the caret across a recomposition that pushes
 * back the value the field already holds.
 */
internal fun JTextComponent.setTextPreservingCaret(value: String) {
    if (text != value) text = value
}
