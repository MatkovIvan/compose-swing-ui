package org.jetbrains.compose.swing.node

import org.jetbrains.compose.swing.components.layout.ScrollablePanel
import javax.swing.JButton
import kotlin.test.Test
import kotlin.test.assertEquals

/** Names used in placement failures must identify caller-visible component types. */
class ChildPlacementFailuresTest {
    @Test
    fun anInternalComponentUsesItsPublicSwingType() {
        assertEquals("JPanel", ScrollablePanel().declaredName)
    }

    @Test
    fun aPublicComponentUsesItsDeclaredType() {
        assertEquals("JButton", JButton().declaredName)
    }
}
