@file:JvmMultifileClass
@file:JvmName("AccessibilityModifiersKt")

package org.jetbrains.compose.swing.modifier

import java.awt.Component
import javax.accessibility.AccessibleRole

/**
 * Advertises [role] as the component's accessible role to assistive technologies. Requires a component
 * that supports a role override; the built-in [org.jetbrains.compose.swing.components.Canvas] does, so
 * a custom drawing surface can present itself as, say, an image or a slider.
 *
 * @param role the accessible role to advertise.
 */
public fun SwingModifier.accessibleRole(role: AccessibleRole): SwingModifier = this then AccessibleRoleElement(role)

/**
 * A component that lets [accessibleRole] override the accessible role it reports to assistive
 * technologies. Implemented by custom drawing surfaces such as
 * [org.jetbrains.compose.swing.components.Canvas], whose intrinsic role is otherwise generic.
 */
public interface AccessibleRoleProvider {
    /**
     * The accessible role this component reports, or `null` to report its intrinsic default. Set by the
     * [accessibleRole] modifier and read by the component's `AccessibleContext`.
     */
    public var accessibleRoleOverride: AccessibleRole?
}

private class AccessibleRoleElement(
    private val role: AccessibleRole,
) : SwingModifier.Element<Component, AccessibleRoleElement.Node> {
    override val targetType: Class<Component> get() = Component::class.java
    override val key: Any get() = PropertyKey.ACCESSIBLE_ROLE

    override fun create(): Node = Node()

    override fun update(node: Node) {
        node.role = role
        node.apply()
    }

    class Node : SwingModifier.Node<Component>() {
        var role: AccessibleRole? = null
        private var original: AccessibleRole? = null

        private fun provider(): AccessibleRoleProvider =
            component as? AccessibleRoleProvider
                ?: error(
                    "accessibleRole requires a component that supports a role override, " +
                        "but the component is a ${component.javaClass.name}",
                )

        override fun onAttach() {
            original = provider().accessibleRoleOverride
        }

        fun apply() {
            provider().accessibleRoleOverride = role
        }

        override fun onDetach() {
            (component as? AccessibleRoleProvider)?.accessibleRoleOverride = original
        }
    }
}
