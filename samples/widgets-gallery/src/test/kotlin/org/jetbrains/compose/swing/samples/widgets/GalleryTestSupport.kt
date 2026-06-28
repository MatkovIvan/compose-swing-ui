package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.SwingUiTest
import javax.swing.JList
import kotlin.test.assertTrue

// Shared navigation helpers for the widgets-gallery section tests. The gallery is a single navigation
// shell (ShowcaseShell) whose sidebar list selects which section body fills the centre, so every
// section test starts by mounting the shell and switching to its section through that list — exactly
// the gesture a user makes. Centralising the switch keeps each test focused on the section's behavior
// rather than re-deriving the navigation by hand.

/**
 * Mounts [ShowcaseShell] and switches the sidebar to the section titled [title], settling the
 * composition so the section body is mounted before returning. Fails if no section carries [title],
 * so a renamed or removed section surfaces as a clear failure rather than a silent no-op.
 *
 * After this returns, the section's body is the live centre content and its `"Section: <title>"`
 * footer label is present, which the callers assert as the section-mounted smoke check.
 */
internal suspend fun SwingUiTest.openSection(title: String) {
    setContent { ShowcaseShell() }
    val index = showcaseSections.indexOfFirst { it.title == title }
    assertTrue(index >= 0, "Section \"$title\" must be registered in showcaseSections")
    val list = onNodeWithTag(SECTION_LIST_TAG).fetch<JList<*>>()
    list.selectedIndex = index
    awaitIdle()
    onNodeWithText("Section: $title", substring = true).assertExists()
}
