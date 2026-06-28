@file:JvmMultifileClass
@file:JvmName("LayoutModifiersKt")

package org.jetbrains.compose.swing.modifier

import java.awt.Component
import java.awt.Dimension

/** Sets `minimumSize` and relays out; `null` restores the layout-computed minimum size. */
public fun SwingModifier.minimumSize(size: Dimension?): SwingModifier =
    this then
        propertyElement<Component, Dimension?>(
            PropertyKey.MINIMUM_SIZE,
            size,
            read = { if (it.isMinimumSizeSet) it.minimumSize else null },
            write = { c, v ->
                c.minimumSize = v
                c.revalidate()
            },
        )

/** Sets `minimumSize` to `Dimension(width, height)` and relays out. */
public fun SwingModifier.minimumSize(
    width: Int,
    height: Int,
): SwingModifier = minimumSize(Dimension(width, height))
