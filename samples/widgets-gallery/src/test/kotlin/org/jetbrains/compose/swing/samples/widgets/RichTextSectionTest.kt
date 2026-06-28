package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.runSwingUiTest
import kotlin.test.Test

/**
 * Behavioral coverage for the Rich text section: an EditorPane that flips its content type between
 * plain text and HTML through a ToggleButton, and a TextPane whose echo reports its length. Both
 * editors bind their value to Compose state, so the toggle and the echo recompose together.
 */
class RichTextSectionTest {
    @Test
    fun theEditorPaneTogglesItsRenderedContentType() =
        runSwingUiTest {
            openSection("Rich text")

            // The EditorPane starts as plain text; the echo and the toggle caption both say so.
            onNodeWithText("Rendered as plain text", substring = true).assertExists()
            onNodeWithText("Content type: PlainText", substring = true).assertExists()

            // Flipping the toggle switches the content type to HTML, which the echo mirrors.
            onNodeWithText("Content type: PlainText", substring = true).performClick()
            onNodeWithText("Rendered as HTML", substring = true).assertExists()
            onNodeWithText("Content type: Html", substring = true).assertExists()
        }

    @Test
    fun theTextPaneEchoesItsInitialLength() =
        runSwingUiTest {
            openSection("Rich text")

            // The TextPane is seeded with a known string; its echo reports that exact length.
            val seeded = "A styled-document editor.\nType here."
            onNodeWithText("Length: ${seeded.length}", substring = true).assertExists()
        }
}
