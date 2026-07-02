package org.jetbrains.compose.swing.components

import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.listener.listener
import java.beans.PropertyChangeListener
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.ListSelectionListener
import javax.swing.text.AbstractDocument
import javax.swing.text.Document
import javax.swing.text.JTextComponent

/*
 * Internal listener helpers for the registration sites that the public typed builders do not reach
 * directly: a table's selection model, and a text component's document across document swaps. Both are
 * built on the single instance-based [listener] seam, so a stable instance is attached once and the
 * same instance removed on detach/reuse.
 */

/**
 * Attaches a [ListSelectionListener] to a `JTable`'s `selectionModel` (where its selection events are
 * published), removing the same instance on detach. The public `listSelectionListener` builder targets
 * `JList`, so a table needs this selection-model-targeted helper.
 */
internal fun SwingModifier.tableSelectionListener(listener: ListSelectionListener): SwingModifier =
    listener<JTable, ListSelectionListener>(
        listener,
        { c, l -> c.selectionModel.addListSelectionListener(l) },
        { c, l -> c.selectionModel.removeListSelectionListener(l) },
    )

/**
 * Attaches [listener] to a `JTextComponent`'s document and follows the component across document swaps:
 * when the component's `document` property changes — as happens when a `JEditorPane` switches content
 * type — the same [listener] is moved onto the new document. Detach removes it from the document the
 * component currently holds and stops following swaps. Covers `JEditorPane`/`JTextPane`, whose document
 * is replaced when the content type changes.
 */
internal fun SwingModifier.swappableDocumentListener(listener: DocumentListener): SwingModifier =
    listener<JTextComponent, SwappableDocumentInstaller>(
        SwappableDocumentInstaller(listener),
        { c, installer -> installer.attach(c) },
        { c, installer -> installer.detach(c) },
    )

/**
 * Keeps a single [documentListener] registered on a text component's *current* document. [attach] adds
 * it to the document held at attach time and registers a swap listener that re-homes it whenever the
 * `document` property changes; [detach] removes both. One installer instance backs one attachment, so it
 * is safe to reuse across re-attachments of the same stable instance.
 */
private class SwappableDocumentInstaller(
    private val documentListener: DocumentListener,
) {
    private val swapListener =
        PropertyChangeListener { event ->
            (event.oldValue as? Document)?.removeDocumentListener(documentListener)
            (event.newValue as? Document)?.addDocumentListener(documentListener)
        }

    fun attach(component: JTextComponent) {
        component.document.addDocumentListener(documentListener)
        component.addPropertyChangeListener("document", swapListener)
    }

    fun detach(component: JTextComponent) {
        component.removePropertyChangeListener("document", swapListener)
        component.document.removeDocumentListener(documentListener)
    }
}

/** Reads the full text of [this] document. */
internal fun Document.fullText(): String = getText(0, length)

/**
 * Replaces the `[offset, offset + length)` region of [this] document with [text]. When the document is
 * an [AbstractDocument] (as `PlainDocument` and the default text-component documents are) the change is
 * applied through its atomic `replace`, so any installed `DocumentFilter` sees one replace; otherwise
 * it falls back to a `remove` followed by an `insertString`.
 */
internal fun Document.replaceSpan(
    offset: Int,
    length: Int,
    text: String,
) {
    when (this) {
        is AbstractDocument -> {
            replace(offset, length, text, null)
        }

        else -> {
            if (length > 0) remove(offset, length)
            if (text.isNotEmpty()) insertString(offset, text, null)
        }
    }
}

/**
 * Collects the selected indices of [this] selection model, in ascending order — the same set
 * `JList.getSelectedIndices`/`JTable.getSelectedRows` derive from a selection model. A list/table
 * selection listener's event source is the selection model, so this reads the settled selection from
 * the event without a reference to the owning component.
 */
internal fun ListSelectionModel.selectedIndices(): List<Int> {
    val min = minSelectionIndex
    if (min < 0) return emptyList()
    return (min..maxSelectionIndex).filter { isSelectedIndex(it) }
}

/**
 * Builds a [DocumentListener] that runs [onChange] with the firing [DocumentEvent] for any
 * insert/remove/attribute change.
 */
internal fun documentChangeListener(onChange: (DocumentEvent) -> Unit): DocumentListener =
    object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent): Unit = onChange(e)

        override fun removeUpdate(e: DocumentEvent): Unit = onChange(e)

        override fun changedUpdate(e: DocumentEvent): Unit = onChange(e)
    }
