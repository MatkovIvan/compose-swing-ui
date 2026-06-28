@file:JvmMultifileClass
@file:JvmName("AccessibilityModifiersKt")

package org.jetbrains.compose.swing.modifier

import java.awt.Component
import java.awt.Container
import java.awt.FocusTraversalPolicy
import javax.swing.JComponent

/**
 * Assigns this component a position in its container's keyboard focus-traversal order. Lower indices
 * are reached first when tabbing forward. Effective only inside a container that installs the
 * composition-order policy via [orderedFocusTraversal]; components without an index follow the indexed
 * ones in their natural order.
 *
 * @param index the traversal position; lower is earlier.
 */
public fun SwingModifier.focusTraversalIndex(index: Int): SwingModifier =
    this then
        propertyElement<JComponent, Any?>(
            PropertyKey.FOCUS_TRAVERSAL_INDEX,
            index,
            read = { it.getClientProperty(FOCUS_TRAVERSAL_INDEX_KEY) },
            write = { c, v -> c.putClientProperty(FOCUS_TRAVERSAL_INDEX_KEY, v) },
        )

/**
 * Makes this container a focus-cycle root whose Tab order follows its children's
 * [focusTraversalIndex] values (ascending), rather than their on-screen geometry. Children without an
 * index are visited after the indexed ones. Requires a `JComponent` target.
 */
public fun SwingModifier.orderedFocusTraversal(): SwingModifier = this then OrderedFocusTraversalElement

/**
 * The `JComponent` client-property key under which [focusTraversalIndex] stores a component's traversal
 * position. Read by the [orderedFocusTraversal] policy to order children.
 */
private const val FOCUS_TRAVERSAL_INDEX_KEY: String = "org.jetbrains.compose.swing.focusTraversalIndex"

private object OrderedFocusTraversalElement :
    SwingModifier.Element<JComponent, OrderedFocusTraversalElement.Node> {
    override val targetType: Class<JComponent> get() = JComponent::class.java
    override val key: Any get() = PropertyKey.ORDERED_FOCUS_TRAVERSAL

    override fun create(): Node = Node()

    override fun update(node: Node): Unit = node.apply()

    class Node : SwingModifier.Node<JComponent>() {
        private var original: SavedFocusTraversal? = null

        override fun onAttach() {
            original =
                SavedFocusTraversal(
                    cycleRoot = component.isFocusCycleRoot,
                    policyProvider = component.isFocusTraversalPolicyProvider,
                    policy = component.focusTraversalPolicy,
                )
        }

        fun apply() {
            component.isFocusCycleRoot = true
            component.isFocusTraversalPolicyProvider = true
            component.focusTraversalPolicy = CompositionOrderFocusTraversalPolicy
        }

        override fun onDetach() {
            val saved = original ?: return
            component.focusTraversalPolicy = saved.policy
            component.isFocusTraversalPolicyProvider = saved.policyProvider
            component.isFocusCycleRoot = saved.cycleRoot
        }
    }
}

private class SavedFocusTraversal(
    val cycleRoot: Boolean,
    val policyProvider: Boolean,
    val policy: FocusTraversalPolicy?,
)

/**
 * Orders a container's focusable descendants by their [focusTraversalIndex] (ascending), falling back
 * to child order for components without an index, which are visited after all indexed ones.
 */
private object CompositionOrderFocusTraversalPolicy : FocusTraversalPolicy() {
    private fun orderedFocusables(aContainer: Container): List<Component> {
        val focusables = mutableListOf<Component>()

        fun isTraversable(child: Component): Boolean =
            child.isFocusable && child.isVisible && child.isEnabled && child.focusTraversalKeysEnabled

        fun collect(container: Container) {
            for (child in container.components) {
                if (isTraversable(child)) focusables += child
                if (child is Container && !child.isFocusCycleRoot) collect(child)
            }
        }
        collect(aContainer)

        // Stable sort: indexed components by ascending index first, un-indexed ones keep their order
        // after them (a missing index sorts as the largest possible position).
        return focusables.sortedBy { component ->
            (component as? JComponent)?.getClientProperty(FOCUS_TRAVERSAL_INDEX_KEY) as? Int ?: Int.MAX_VALUE
        }
    }

    override fun getComponentAfter(
        aContainer: Container,
        aComponent: Component,
    ): Component? {
        val ordered = orderedFocusables(aContainer)
        if (ordered.isEmpty()) return null
        val current = ordered.indexOf(aComponent)
        return ordered[(current + 1) % ordered.size]
    }

    override fun getComponentBefore(
        aContainer: Container,
        aComponent: Component,
    ): Component? {
        val ordered = orderedFocusables(aContainer)
        if (ordered.isEmpty()) return null
        val current = ordered.indexOf(aComponent)
        return ordered[(current - 1 + ordered.size) % ordered.size]
    }

    override fun getFirstComponent(aContainer: Container): Component? = orderedFocusables(aContainer).firstOrNull()

    override fun getLastComponent(aContainer: Container): Component? = orderedFocusables(aContainer).lastOrNull()

    override fun getDefaultComponent(aContainer: Container): Component? = getFirstComponent(aContainer)
}
