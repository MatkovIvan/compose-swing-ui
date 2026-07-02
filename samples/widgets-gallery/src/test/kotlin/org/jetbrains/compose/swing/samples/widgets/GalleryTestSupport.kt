package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.SwingMatcher
import org.jetbrains.compose.swing.test.SwingUiTest
import javax.swing.JList
import kotlin.test.assertTrue

internal suspend fun SwingUiTest.openSection(title: String) {
    setContent { ShowcaseShell() }
    val index = showcaseSections.indexOfFirst { it.title == title }
    assertTrue(index >= 0, "Section \"$title\" must be registered in showcaseSections")
    val list = onSectionList().fetch<JList<*>>()
    list.selectedIndex = index
    awaitIdle()
    onNodeWithText("Section: $title", substring = true).assertExists()
}

internal fun SwingUiTest.onSectionList() = onNode(SwingMatcher.hasAccessibleName("Sections"))
