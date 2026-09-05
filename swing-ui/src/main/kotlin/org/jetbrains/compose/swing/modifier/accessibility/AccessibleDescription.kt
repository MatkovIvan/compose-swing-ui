@file:JvmMultifileClass
@file:JvmName("AccessibilityModifierKt")

package org.jetbrains.compose.swing.modifier.accessibility

import org.jetbrains.annotations.Nls
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.propertyElement
import java.awt.Component

/**
 * Sets the component's accessible description - a longer localized explanation assistive technologies
 * can read after the name. `null` clears any description this modifier set.
 *
 * A component holds no description of its own until one is set, and its accessible context answers with
 * what it can derive instead: its tooltip, or the description of the caption labeling it. Dropping this
 * modifier hands the description back to that derivation rather than pinning the string it derived. A
 * description the component was built carrying is put back instead, since that one is its own.
 *
 * @param description the accessible description to advertise, or `null` to clear it.
 * @return this chain with the accessible description declared on it.
 * @see javax.accessibility.AccessibleContext.setAccessibleDescription
 */
public fun SwingModifier.accessibleDescription(description: @Nls String?): SwingModifier =
    this then
        propertyElement<Component, String?>(
            name = "accessibleDescription",
            value = description,
            read = { it.accessibleContext?.declaredAccessibleDescription() },
            write = { component, value -> component.accessibleContext?.accessibleDescription = value },
        )
