@file:JvmMultifileClass
@file:JvmName("LayoutModifiersKt")

package org.jetbrains.compose.swing.modifier

import java.awt.Component
import java.awt.Dimension

/** Sets `maximumSize` and relays out; `null` restores the layout-computed maximum size. */
public fun SwingModifier.maximumSize(size: Dimension?): SwingModifier =
    this then
        propertyElement<Component, Dimension?>(
            PropertyKey.MAXIMUM_SIZE,
            size,
            read = { if (it.isMaximumSizeSet) it.maximumSize else null },
            write = { c, v ->
                c.maximumSize = v
                c.revalidate()
            },
        )

/** Sets `maximumSize` to `Dimension(width, height)` and relays out. */
public fun SwingModifier.maximumSize(
    width: Int,
    height: Int,
): SwingModifier = maximumSize(Dimension(width, height))
