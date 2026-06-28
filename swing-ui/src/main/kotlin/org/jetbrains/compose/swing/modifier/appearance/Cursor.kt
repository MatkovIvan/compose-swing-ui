@file:JvmMultifileClass
@file:JvmName("AppearanceModifiersKt")

package org.jetbrains.compose.swing.modifier.appearance

import org.jetbrains.compose.swing.modifier.PropertyKey
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.propertyElement
import java.awt.Component
import java.awt.Cursor

/** Sets `cursor`; `null` restores the inherited cursor. */
public fun SwingModifier.cursor(cursor: Cursor?): SwingModifier =
    this then
        propertyElement<Component, Cursor?>(
            PropertyKey.CURSOR,
            cursor,
            read = { it.cursor },
            write = { c, v -> c.cursor = v },
        )
