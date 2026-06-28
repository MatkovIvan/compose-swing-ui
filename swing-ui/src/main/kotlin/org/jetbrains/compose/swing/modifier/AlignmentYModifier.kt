@file:JvmMultifileClass
@file:JvmName("LayoutModifiersKt")

package org.jetbrains.compose.swing.modifier

import javax.swing.JComponent

/**
 * Sets the vertical alignment used to position the component along the y axis, where `0.0` aligns to
 * the top, `0.5` centers, and `1.0` aligns to the bottom. A parent that honors alignment — such as a
 * horizontal `BoxLayout` — lines its children up by this value, so giving siblings the same alignment
 * keeps them on one row.
 */
public fun SwingModifier.alignmentY(value: Float): SwingModifier =
    this then
        propertyElement<JComponent, Float>(
            PropertyKey.ALIGNMENT_Y,
            value,
            read = { it.alignmentY },
            write = { c, v ->
                c.alignmentY = v
                c.revalidate()
            },
        )
