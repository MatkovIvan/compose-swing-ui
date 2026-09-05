@file:JvmMultifileClass
@file:JvmName("AccessibilityModifierKt")

package org.jetbrains.compose.swing.modifier.accessibility

import org.jetbrains.annotations.Nls
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.propertyElement
import java.awt.Component

/**
 * Sets the component's accessible name - the short localized string assistive technologies announce
 * for it. `null` clears any name this modifier set. Mirrors Compose's
 * `semantics { contentDescription = ... }`.
 *
 * A component holds no name of its own until one is set, and its accessible context answers with what it
 * can derive instead: a button's or a label's text, a titled border, the caption labeling it. Dropping
 * this modifier hands the name back to that derivation rather than pinning the string it derived. A name
 * the component was built carrying is put back instead, since that one is its own.
 *
 * @param name the accessible name to advertise, or `null` to clear it.
 * @return this chain with the accessible name declared on it.
 * @see javax.accessibility.AccessibleContext.setAccessibleName
 */
public fun SwingModifier.accessibleName(name: @Nls String?): SwingModifier =
    this then
        propertyElement<Component, String?>(
            name = "accessibleName",
            value = name,
            read = { it.accessibleContext?.declaredAccessibleName() },
            write = { component, value -> component.accessibleContext?.accessibleName = value },
        )
