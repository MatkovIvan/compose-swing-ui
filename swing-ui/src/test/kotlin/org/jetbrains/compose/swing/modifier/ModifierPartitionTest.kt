package org.jetbrains.compose.swing.modifier

import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.layout.LayoutElement
import org.jetbrains.compose.swing.modifier.layout.alignmentX
import org.jetbrains.compose.swing.modifier.layout.alignmentY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModifierPartitionTest {
    @Test
    fun layoutChainIsEmptyWithoutLayoutElementsAndPreservesDeclarationOrder() {
        val partition = ModifierPartition()
        SwingModifier.testTag("tag").foldIn(partition) { current, element ->
            current.take(element)
            current
        }
        assertTrue(partition.layoutChain.isEmpty())

        val modifier = SwingModifier.alignmentX(0f).then(SwingModifier.alignmentY(1f))
        val expected = mutableListOf<LayoutElement>()
        modifier.foldIn(partition) { current, element ->
            if (element is LayoutElement) expected.add(element)
            current.take(element)
            current
        }

        assertEquals(expected, partition.layoutChain)
    }
}
