@file:JvmMultifileClass
@file:JvmName("LayoutModifiersKt")

package org.jetbrains.compose.swing.modifier.layout

import org.jetbrains.compose.swing.modifier.PropertyElement
import org.jetbrains.compose.swing.modifier.SwingModifier
import java.awt.Component
import java.awt.Dimension

/** Sets `preferredSize` and relays out; `null` restores the layout-computed preferred size. */
public fun SwingModifier.preferredSize(size: Dimension?): SwingModifier = this then PreferredSizeElement(size)

/** Sets `preferredSize` to `Dimension(width, height)` and relays out. */
public fun SwingModifier.preferredSize(
    width: Int,
    height: Int,
): SwingModifier = preferredSize(Dimension(width, height))

private class PreferredSizeElement(
    size: Dimension?,
) : PropertyElement<Component, Dimension?>(
        Component::class.java,
        size,
        read = { if (it.isPreferredSizeSet) it.preferredSize else null },
        write = { c, v ->
            c.preferredSize = v
            c.revalidate()
        },
    )

/** Sets `minimumSize` and relays out; `null` restores the layout-computed minimum size. */
public fun SwingModifier.minimumSize(size: Dimension?): SwingModifier = this then MinimumSizeElement(size)

/** Sets `minimumSize` to `Dimension(width, height)` and relays out. */
public fun SwingModifier.minimumSize(
    width: Int,
    height: Int,
): SwingModifier = minimumSize(Dimension(width, height))

private class MinimumSizeElement(
    size: Dimension?,
) : PropertyElement<Component, Dimension?>(
        Component::class.java,
        size,
        read = { if (it.isMinimumSizeSet) it.minimumSize else null },
        write = { c, v ->
            c.minimumSize = v
            c.revalidate()
        },
    )

/** Sets `maximumSize` and relays out; `null` restores the layout-computed maximum size. */
public fun SwingModifier.maximumSize(size: Dimension?): SwingModifier = this then MaximumSizeElement(size)

/** Sets `maximumSize` to `Dimension(width, height)` and relays out. */
public fun SwingModifier.maximumSize(
    width: Int,
    height: Int,
): SwingModifier = maximumSize(Dimension(width, height))

private class MaximumSizeElement(
    size: Dimension?,
) : PropertyElement<Component, Dimension?>(
        Component::class.java,
        size,
        read = { if (it.isMaximumSizeSet) it.maximumSize else null },
        write = { c, v ->
            c.maximumSize = v
            c.revalidate()
        },
    )
