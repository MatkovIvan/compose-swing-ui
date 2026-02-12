package org.jetbrains.compose.swing

import androidx.compose.runtime.AbstractApplier
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container

/**
 * Applier for Swing component tree.
 * Manages the hierarchy of Swing components in a Compose-like way.
 */
internal class SwingApplier(root: Container) : AbstractApplier<Component>(root) {
    
    override fun insertTopDown(index: Int, instance: Component) {
        // Swing components are inserted bottom-up
    }

    override fun insertBottomUp(index: Int, instance: Component) {
        val container = current as? Container
            ?: error("Current node $current is not a Container, cannot add child $instance")
        
        // Auto-detect BorderLayout and assign constraints based on index
        if (container.layout is BorderLayout) {
            val constraint = when (index) {
                0 -> BorderLayout.NORTH
                1 -> BorderLayout.CENTER
                2 -> BorderLayout.SOUTH
                3 -> BorderLayout.WEST
                4 -> BorderLayout.EAST
                else -> BorderLayout.CENTER
            }
            container.add(instance, constraint)
        } else {
            container.add(instance, index)
        }
    }

    override fun remove(index: Int, count: Int) {
        val container = current as? Container
            ?: error("Current node $current is not a Container, cannot remove children")
        repeat(count) {
            container.remove(index)
        }
    }

    override fun move(from: Int, to: Int, count: Int) {
        val container = current as? Container
            ?: error("Current node $current is not a Container, cannot move children")
        
        if (from == to) return
        
        val components = mutableListOf<Component>()
        repeat(count) {
            components.add(container.getComponent(from))
            container.remove(from)
        }
        
        val insertIndex = if (from < to) to - count else to
        components.forEachIndexed { offset, component ->
            container.add(component, insertIndex + offset)
        }
    }

    override fun onClear() {
        (root as? Container)?.removeAll()
    }
}
