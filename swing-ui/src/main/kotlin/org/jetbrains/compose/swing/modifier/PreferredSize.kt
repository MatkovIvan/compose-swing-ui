@file:JvmMultifileClass
@file:JvmName("LayoutModifiersKt")

package org.jetbrains.compose.swing.modifier

import java.awt.Component
import java.awt.Dimension

/** Sets `preferredSize` and relays out; `null` restores the layout-computed preferred size. */
public fun SwingModifier.preferredSize(size: Dimension?): SwingModifier =
    this then
        propertyElement<Component, Dimension?>(
            PropertyKey.PREFERRED_SIZE,
            size,
            read = { if (it.isPreferredSizeSet) it.preferredSize else null },
            write = { c, v ->
                c.preferredSize = v
                c.revalidate()
            },
        )

/** Sets `preferredSize` to `Dimension(width, height)` and relays out. */
public fun SwingModifier.preferredSize(
    width: Int,
    height: Int,
): SwingModifier = preferredSize(Dimension(width, height))
