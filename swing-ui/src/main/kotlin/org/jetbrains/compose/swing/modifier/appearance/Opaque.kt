@file:JvmMultifileClass
@file:JvmName("AppearanceModifierKt")

package org.jetbrains.compose.swing.modifier.appearance

import org.jetbrains.compose.swing.modifier.PropertyAccessors
import org.jetbrains.compose.swing.modifier.PropertyInterference
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.propertyElement
import javax.swing.AbstractButton
import javax.swing.JComponent
import javax.swing.JMenuItem

/**
 * Sets `isOpaque` - required for [background] to actually paint. Requires a `JComponent` target.
 *
 * The declaration stands over a component that computes the flag for itself: a look and feel's button
 * listener writes the flag from [contentAreaFilled] on every change to that property.
 *
 * A button holds no flag of its own while it agrees with [contentAreaFilled], since that is the rule a
 * look and feel keeps it at, so removing the declaration from one hands the flag back to that rule
 * rather than to the value read when the declaration attached. A menu item is left out of it: nothing
 * ties its flag to the fill, so what it carries is its own.
 *
 * @param opaque `true` promises the component paints every pixel of its bounds, letting Swing skip what is
 *   behind it; `false` lets the parent show through.
 * @return this chain with the opaque flag declared on it.
 * @see javax.swing.JComponent.setOpaque
 */
public fun SwingModifier.opaque(opaque: Boolean): SwingModifier =
    this then
        propertyElement<JComponent, Boolean?>(
            name = "opaque",
            value = opaque,
            read = OpaqueProperty.read,
            write = OpaqueProperty.write,
            // The component announces every change of the flag, its own included, so the flag itself is
            // what to listen on.
            interference = PropertyInterference.OverwrittenOn("opaque"),
        )

/**
 * The flag's own accessors. The read answers null for a button whose flag follows its fill, which is the
 * flag being none of the button's own; only such a button is what a null write resolves against, so the
 * cast there holds.
 */
internal val OpaqueProperty =
    PropertyAccessors<JComponent, Boolean?>(
        read = {
            val derived = (it as? AbstractButton)?.takeIf { button -> button !is JMenuItem }?.isContentAreaFilled
            if (it.isOpaque == derived) null else it.isOpaque
        },
        write = { component, value ->
            component.isOpaque = value ?: (component as AbstractButton).isContentAreaFilled
        },
    )
