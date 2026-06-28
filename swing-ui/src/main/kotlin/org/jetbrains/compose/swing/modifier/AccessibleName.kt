@file:JvmMultifileClass
@file:JvmName("AccessibilityModifiersKt")

package org.jetbrains.compose.swing.modifier

import java.awt.Component

/**
 * Sets the component's accessible name — the short localized string assistive technologies announce
 * for it. `null` clears any name this modifier set. Mirrors Compose's
 * `semantics { contentDescription = … }`.
 *
 * @param name the accessible name to advertise, or `null` to clear it.
 */
public fun SwingModifier.accessibleName(name: String?): SwingModifier = this then AccessibleNameElement(name)

private class AccessibleNameElement(
    private val name: String?,
) : SwingModifier.Element<Component, AccessibleNameElement.Node> {
    override val targetType: Class<Component> get() = Component::class.java
    override val key: Any get() = PropertyKey.ACCESSIBLE_NAME

    override fun create(): Node = Node()

    override fun update(node: Node) {
        node.name = name
        node.apply()
    }

    class Node : SwingModifier.Node<Component>() {
        var name: String? = null
        private var original: String? = null

        override fun onAttach() {
            original = component.accessibleContext?.accessibleName
        }

        fun apply() {
            component.accessibleContext?.accessibleName = name
        }

        override fun onDetach() {
            component.accessibleContext?.accessibleName = original
        }
    }
}
