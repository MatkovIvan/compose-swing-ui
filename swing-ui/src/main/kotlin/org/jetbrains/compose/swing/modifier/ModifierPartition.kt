package org.jetbrains.compose.swing.modifier

import org.jetbrains.compose.swing.modifier.layout.ConstraintElement
import org.jetbrains.compose.swing.modifier.layout.LayoutElement
import org.jetbrains.compose.swing.modifier.layout.SlotElement
import org.jetbrains.compose.swing.modifier.layout.twoKindsOfConstraint

/**
 * One modifier taken apart as the walk over it reaches each entry: the elements the diff has nodes for,
 * split into the keyed ones and the additive ones, and the declarations the node holder reads instead.
 *
 * Built by the walk and dropped after it, so nothing of one pass outlives the pass that made it.
 */
internal class ModifierPartition {
    /** The keyed (last-wins) elements of the modifier being applied, by [SwingModifier.NodeElement.key]. */
    val keyed: LinkedHashMap<Any, SwingModifier.NodeElement<*, *>> = LinkedHashMap()

    /** The additive (subscription) elements of the modifier being applied, in declaration order. */
    val additive: ArrayList<SwingModifier.NodeElement<*, *>> = ArrayList()

    /**
     * The host slot the modifier being applied declares, the last one its walk reaches. `null` where it
     * declares none, which puts a node that has given up its slot back to placement by index.
     */
    var slot: SlotElement? = null
        private set

    /**
     * The layout constraint the modifier being applied declares, folded together as the walk reaches each
     * element that states part of it. `null` where it declares none.
     */
    var constraint: Any? = null
        private set

    private var mutableLayoutChain: MutableLayoutChain? = null

    /**
     * The layout elements the modifier being applied declares, in declaration order - a chain the child's
     * container measures it through, outermost first. Empty where it declares none.
     *
     * Separate from [constraint] because the two fold differently and neither can stand for the other: a
     * constraint names a place in one parent and is resolved last-of-its-kind, while these compose.
     */
    val layoutChain: List<LayoutElement> get() = mutableLayoutChain.orEmpty()

    /**
     * Whether the constraint reached so far was stated whole rather than in parts, or `null` where the
     * walk has reached none. A modifier mixing the two names a place in a parent that holds its children
     * the other way, and is refused.
     */
    private var constraintStatedWhole: Boolean? = null

    /**
     * The keys the modifier ties its application to, in the order they stand, or `null` where it
     * declares none - which is a key of its own: a modifier that gives its keys up is applied from
     * scratch the way one that changes them is.
     */
    val keys: List<Any?>? get() = if (keyElements.isEmpty()) null else keyElements.flatMap { it.tokens }

    /** The key declarations the walk has taken, so one declared twice is not counted twice. */
    private var keyElements: List<KeyElement> = emptyList()

    /** Routes one entry of the modifier being applied to whatever reads it. */
    fun take(element: SwingModifier.Element) {
        when (element) {
            is SwingModifier.NodeElement<*, *> -> {
                takeElement(element)
            }

            // The last slot declared is the one the component is installed into.
            is SlotElement -> {
                slot = element
            }

            is ConstraintElement -> {
                takeConstraint(element)
            }

            // Ordered rather than resolved: a chain of these is applied in the order it was declared.
            is LayoutElement -> {
                mutableLayoutChain?.add(element) ?: MutableLayoutChain(element).also { mutableLayoutChain = it }
            }

            is KeyElement -> {
                takeKeys(element)
            }

            else -> {
                throw IllegalArgumentException(
                    "A modifier entry is either a SwingModifier.NodeElement, which carries the node that " +
                        "writes onto the component, or one of the declarations this library reads itself. " +
                        "${element.javaClass.name} is neither, and nothing would apply it. Extend " +
                        "SwingModifier.NodeElement instead.",
                )
            }
        }
    }

    private fun takeElement(element: SwingModifier.NodeElement<*, *>) {
        if (element.additive) {
            additive.add(element)
            return
        }
        // Last wins the place as well as the value: the modifier settles two writes of one property by its
        // own order, so a key declared again later stands where it was declared last. The key is asked for
        // once, which is what the walk over a modifier costs.
        val key = element.key
        keyed.remove(key)
        keyed[key] = element
    }

    /**
     * The parts of one constraint are declared one at a time, so they fold together in the order the
     * modifier declares them; one stating the whole constraint replaces what came before.
     */
    private fun takeConstraint(element: ConstraintElement) {
        val statedWhole = element.statesWholeConstraint
        require(constraintStatedWhole.let { it == null || it == statedWhole }) { twoKindsOfConstraint() }
        constraintStatedWhole = statedWhole
        constraint = element.foldInto(constraint)
    }

    /**
     * Declaring again adds keys, so a builder tying its own writes to a key keeps its caller's. One
     * declaration repeated adds nothing the modifier is not already tied to.
     */
    private fun takeKeys(element: KeyElement) {
        // Last wins the place as well as the value, the way a property key does: a declaration repeated
        // stands where it was declared last, and counts once.
        keyElements = keyElements.filterNot { it == element } + element
    }

    private class MutableLayoutChain(
        first: LayoutElement,
    ) : ArrayList<LayoutElement>(1) {
        init {
            add(first)
        }
    }
}
