@file:JvmMultifileClass
@file:JvmName("LayoutModifiersKt")

package org.jetbrains.compose.swing.modifier

import javax.swing.JComponent

/**
 * Sets the horizontal alignment used to position the component along the x axis, where `0.0` aligns to
 * the left, `0.5` centers, and `1.0` aligns to the right. A parent that honors alignment — such as a
 * vertical `BoxLayout` — lines its children up by this value, so giving siblings the same alignment
 * keeps them in one column.
 */
public fun SwingModifier.alignmentX(value: Float): SwingModifier =
    this then
        propertyElement<JComponent, Float>(
            PropertyKey.ALIGNMENT_X,
            value,
            read = { it.alignmentX },
            write = { c, v ->
                c.alignmentX = v
                c.revalidate()
            },
        )
