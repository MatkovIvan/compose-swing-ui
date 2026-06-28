@file:JvmMultifileClass
@file:JvmName("LayoutModifiersKt")

package org.jetbrains.compose.swing.modifier

import java.awt.Component

/**
 * Sets `isVisible` — whether the component is shown in its parent's layout.
 *
 * A hidden component stays attached to its parent and keeps its full native state (selection, scroll
 * position, focus history, model), so toggling it back on recreates nothing. Reach for `visible` when
 * you only need to hide a component that already exists.
 */
public fun SwingModifier.visible(visible: Boolean): SwingModifier =
    this then
        propertyElement<Component, Boolean>(
            PropertyKey.VISIBLE,
            visible,
            read = { it.isVisible },
            write = { c, v -> c.isVisible = v },
        )
