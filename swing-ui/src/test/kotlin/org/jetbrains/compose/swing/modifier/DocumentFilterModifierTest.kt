package org.jetbrains.compose.swing.modifier

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.text.EditorPane
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.interaction.documentFilter
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.runSwingUiTest
import javax.swing.JEditorPane
import javax.swing.JTextField
import javax.swing.text.AbstractDocument
import javax.swing.text.DocumentFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Behavioral coverage for the `SwingModifier.documentFilter` seam. Each test drives the live
 * `JTextField`'s document the way a typed edit would and asserts what an observer sees: a rejected
 * edit leaves the document unchanged, an accepted (rewritten) edit lands, and clearing the filter
 * restores unfiltered editing.
 */
class DocumentFilterModifierTest {
    /** Accepts only digit characters, dropping any non-digit from an insert or replace. */
    private object DigitsOnlyFilter : DocumentFilter() {
        override fun insertString(
            fb: FilterBypass,
            offset: Int,
            string: String?,
            attr: javax.swing.text.AttributeSet?,
        ) {
            fb.insertString(offset, string?.filter(Char::isDigit).orEmpty(), attr)
        }

        override fun replace(
            fb: FilterBypass,
            offset: Int,
            length: Int,
            text: String?,
            attrs: javax.swing.text.AttributeSet?,
        ) {
            fb.replace(offset, length, text?.filter(Char::isDigit).orEmpty(), attrs)
        }
    }

    @Test
    fun filterIsInstalledOnTheDocument() = runSwingUiTest {
        setContent {
            TextField(
                value = "",
                modifier = SwingModifier.testTag("f").documentFilter(DigitsOnlyFilter),
            )
        }
        val document = onNodeWithTag("f").fetch<JTextField>().document as AbstractDocument
        assertEquals(DigitsOnlyFilter, document.documentFilter)
    }

    @Test
    fun validInputPassesThroughTheFilter() = runSwingUiTest {
        val reported = mutableListOf<String>()
        setContent {
            TextField(
                value = "",
                modifier = SwingModifier.testTag("f").documentFilter(DigitsOnlyFilter),
                onValueChange = { reported += it },
            )
        }
        val field = onNodeWithTag("f").fetch<JTextField>()
        field.document.insertString(0, "123", null)
        awaitIdle()

        assertEquals("123", field.text, "valid input should reach the field unchanged")
        assertEquals("123", reported.last(), "onValueChange should report the valid input")
    }

    @Test
    fun invalidCharactersAreGatedOut() = runSwingUiTest {
        setContent {
            TextField(
                value = "",
                modifier = SwingModifier.testTag("f").documentFilter(DigitsOnlyFilter),
            )
        }
        val field = onNodeWithTag("f").fetch<JTextField>()
        // Mixed input: the filter keeps the digits and drops the letters.
        field.document.insertString(0, "a1b2c3", null)
        awaitIdle()

        assertEquals("123", field.text)
    }

    @Test
    fun clearingTheFilterRestoresUnfilteredEditing() = runSwingUiTest {
        var filtered by mutableStateOf(true)
        setContent {
            TextField(
                value = "",
                modifier =
                    SwingModifier
                        .testTag("f")
                        .documentFilter(if (filtered) DigitsOnlyFilter else null),
            )
        }
        val document = onNodeWithTag("f").fetch<JTextField>().document as AbstractDocument
        assertEquals(DigitsOnlyFilter, document.documentFilter, "the filter should be installed while present")

        filtered = false
        awaitIdle()
        assertNull(document.documentFilter, "clearing the modifier should remove the document filter")

        // With the filter gone, previously rejected characters now land.
        val field = onNodeWithTag("f").fetch<JTextField>()
        field.document.insertString(0, "abc", null)
        awaitIdle()
        assertEquals("abc", field.text, "previously rejected characters should now land unfiltered")
    }

    @Test
    fun aDocumentSwapKeepsTheFilterActive() = runSwingUiTest {
        var contentType by mutableStateOf("text/plain")
        setContent {
            EditorPane(
                value = "",
                modifier = SwingModifier.testTag("e").documentFilter(DigitsOnlyFilter),
                contentType = contentType,
            )
        }
        val pane = onNodeWithTag("e").fetch<JEditorPane>()
        val before = pane.document as AbstractDocument
        assertSame(DigitsOnlyFilter, before.documentFilter, "the filter should start on the original document")

        // Switching content type installs a fresh document; the filter must follow onto it rather
        // than being left behind on the old one.
        contentType = "text/html"
        awaitIdle()
        val after = onNodeWithTag("e").fetch<JEditorPane>().document as AbstractDocument
        assertSame(DigitsOnlyFilter, after.documentFilter, "the filter should migrate onto the new document")
        assertNull(before.documentFilter, "the old document's filter must be released on the swap")

        // The migrated filter still gates edits on the new document: the letters are dropped and
        // only the digits reach the document.
        after.insertString(0, "a1b2", null)
        awaitIdle()
        assertEquals(
            "12",
            after.getText(0, after.length),
            "the migrated filter should still gate edits on the new document",
        )
    }
}
