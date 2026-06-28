package org.jetbrains.compose.swing.components

/**
 * A monotonic source of synthetic ids for a declarative scope. Each declaration takes [next] in call
 * order, giving every child a stable composition key independent of its index in the list, so a child
 * keeps its slot (and the state inside it) even when surrounding declarations shift around it.
 */
internal class SyntheticIds {
    private var nextId = 0

    /** Returns the next id in call order. */
    fun next(): Int = nextId++
}
