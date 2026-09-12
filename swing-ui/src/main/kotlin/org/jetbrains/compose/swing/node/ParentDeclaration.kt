package org.jetbrains.compose.swing.node

import org.jetbrains.compose.swing.foundation.layout.MeasurePolicyLayout
import org.jetbrains.compose.swing.foundation.layout.replaceLayoutConstraint
import org.jetbrains.compose.swing.modifier.layout.LayoutElement
import java.awt.Component
import java.awt.Container
import java.awt.LayoutManager2

/**
 * What one node declares to the layout manager of whatever parent holds it: the constraint it is
 * registered under, and the layout modifiers it is measured through.
 *
 * Both record what the manager currently holds as well as what the composition declared, so the two
 * always agree; both are written before the applier attaches the component.
 */
internal class ParentDeclaration(
    private val node: SwingNodeHolder<*>,
) {
    private val component: Component get() = node.component

    /**
     * The container the applier attached the component to, or `null` before it has attached it.
     *
     * This is where the composition put the component, which is not always where it stands: a look and
     * feel moves one of its own accord, as `BasicToolBarUI` does for a tool bar the user drags out.
     */
    private var host: Container? = null

    /**
     * The layout constraint the component is placed under, such as a `BorderLayout` region.
     * `null` means the component is placed by index only.
     *
     * This also records what the parent's layout manager currently holds. The modifier chain sets
     * it before the applier attaches the component, so the two always agree.
     */
    var constraint: Any? = null
        private set

    /**
     * Places this node under [value], the constraint its modifier chain declares, or `null` for none.
     *
     * The value is what a parent's layout manager registers the component under. A manager that does
     * not read it lays the component out as one placed by index alone.
     */
    fun applyConstraint(value: Any?) {
        if (value == constraint) return
        constraint = value
        reapply()
    }

    /**
     * The layout modifiers the component is measured through, outermost first, or empty where its
     * modifier declares none. Records what the parent's manager currently holds, as [constraint] does.
     */
    var layoutChain: List<LayoutElement> = emptyList()
        private set

    /**
     * Measures this node through [value], the layout modifiers its chain declares.
     *
     * Compared by value: an equal chain is the chain already standing, and redeclaring it would
     * rebuild the measurable the parent's manager holds for this component on every pass.
     *
     * A chain the parent measures its child through decides what that child occupies, so a change to it
     * relays the parent out, the way [applyConstraint] does through [reapply].
     */
    fun applyLayoutChain(value: List<LayoutElement>) {
        if (value == layoutChain) return
        layoutChain = value
        checkParentMeasuresChild()
        declareLayoutChain()
        component.revalidate()
    }

    /**
     * Holds a chain the composition declares to the host that would have to read it, which is the host
     * the applier attached the component to rather than whatever container it stands under now.
     *
     * A chain declared before the applier attaches the component - a node composed this pass, a node the
     * composition is relocating - answers to the host it joins as it joins it.
     */
    private fun checkParentMeasuresChild() {
        if (layoutChain.isEmpty()) return
        val host = host ?: return
        check(host.layout is MeasurePolicyLayout) { hostCannotMeasureChild(host, node) }
    }

    /**
     * Tells the parent's layout manager which layout modifiers the component is measured through.
     *
     * A manager that cannot measure a child under constraints has nothing to declare one to, and the
     * chain is dropped rather than refused: this also runs where a look and feel re-registers a
     * component of its own accord, into whatever container it chose. What the composition itself
     * declares is held to [checkParentMeasuresChild] first.
     */
    private fun declareLayoutChain() {
        val manager = component.parent?.layout as? MeasurePolicyLayout ?: return
        manager.declareLayoutChain(component, layoutChain)
    }

    /**
     * Takes [host] as the container the applier has just added the component to, and declares the chain
     * to its manager.
     *
     * Declared after the add, never before: an add gives the host's manager a fresh measurable, and the
     * chain lives on it. Declaring first would leave every add - the applier's own, and the one a
     * constraint change makes - quietly taking the child's layout modifiers away with it.
     */
    fun attachedUnder(host: Container) {
        this.host = host
        declareLayoutChain()
    }

    /**
     * Tells the parent's layout manager that the component uses [constraint].
     *
     * The component is never removed from its parent, so it keeps its position, its focus and its
     * native resources. Only the placement changes.
     *
     * Most managers need `removeLayoutComponent` first because they store the child under its old
     * constraint. `BorderLayout` is one: without this it would hold the component twice. A
     * [MeasurePolicyLayout] retains its child measurable instead, because its constraint is metadata
     * on that measurable and its modifier chain must survive the replacement.
     *
     * A node that is not attached yet is placed by the applier's own add instead, and a parent
     * with no layout manager has nothing to register.
     *
     * Called on its own where a look and feel re-registers a component of its own accord -
     * `BasicToolBarUI` docking a tool bar onto the edge the user dropped it on - which changes what
     * the manager holds without changing what the composition declares. [applyConstraint] writes
     * only what changed and so has nothing to say there.
     */
    fun reapply() {
        val parent = component.parent ?: return
        val manager = parent.layout ?: return
        val declared = constraint
        if (manager is MeasurePolicyLayout) {
            manager.replaceLayoutConstraint(component, declared)
        } else {
            manager.removeLayoutComponent(component)
            if (manager is LayoutManager2) {
                manager.addLayoutComponent(component, declared)
            } else if (declared is String) {
                manager.addLayoutComponent(declared, component)
            }
            declareLayoutChain()
        }
        parent.revalidate()
    }
}
