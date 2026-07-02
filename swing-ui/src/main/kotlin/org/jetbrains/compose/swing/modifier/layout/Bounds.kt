@file:JvmMultifileClass
@file:JvmName("LayoutModifiersKt")

package org.jetbrains.compose.swing.modifier.layout

import org.jetbrains.compose.swing.modifier.PropertyElement
import org.jetbrains.compose.swing.modifier.SwingModifier
import java.awt.Component
import java.awt.Rectangle

/**
 * Sets the component's `bounds` — its position and size within its parent. Effective in a parent that
 * does not lay its children out (a null layout, or a `LayeredPane`), where each child positions itself.
 *
 * @param x the left edge relative to the parent
 * @param y the top edge relative to the parent
 * @param width the width
 * @param height the height
 */
public fun SwingModifier.bounds(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
): SwingModifier = this then BoundsElement(Rectangle(x, y, width, height))

private class BoundsElement(
    bounds: Rectangle,
) : PropertyElement<Component, Rectangle>(
        Component::class.java,
        bounds,
        read = { it.bounds },
        write = { c, v -> c.bounds = v },
    )
