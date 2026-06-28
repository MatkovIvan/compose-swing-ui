@file:JvmMultifileClass
@file:JvmName("AccessibilityModifiersKt")

package org.jetbrains.compose.swing.modifier

import java.awt.Component

/**
 * Sets the component's accessible description — a longer localized explanation assistive technologies
 * can read after the name. `null` clears any description this modifier set.
 *
 * @param description the accessible description to advertise, or `null` to clear it.
 */
public fun SwingModifier.accessibleDescription(description: String?): SwingModifier =
    this then AccessibleDescriptionElement(description)

private class AccessibleDescriptionElement(
    private val description: String?,
) : SwingModifier.Element<Component, AccessibleDescriptionElement.Node> {
    override val targetType: Class<Component> get() = Component::class.java
    override val key: Any get() = PropertyKey.ACCESSIBLE_DESCRIPTION

    override fun create(): Node = Node()

    override fun update(node: Node) {
        node.description = description
        node.apply()
    }

    class Node : SwingModifier.Node<Component>() {
        var description: String? = null
        private var original: String? = null

        override fun onAttach() {
            original = component.accessibleContext?.accessibleDescription
        }

        fun apply() {
            component.accessibleContext?.accessibleDescription = description
        }

        override fun onDetach() {
            component.accessibleContext?.accessibleDescription = original
        }
    }
}
