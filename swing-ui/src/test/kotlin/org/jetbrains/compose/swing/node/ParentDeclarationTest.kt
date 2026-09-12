package org.jetbrains.compose.swing.node

import org.jetbrains.compose.swing.foundation.layout.Alignment
import org.jetbrains.compose.swing.foundation.layout.BoxScopeImpl
import org.jetbrains.compose.swing.foundation.layout.Constraints
import org.jetbrains.compose.swing.foundation.layout.Measurable
import org.jetbrains.compose.swing.foundation.layout.MeasurePolicy
import org.jetbrains.compose.swing.foundation.layout.MeasureResult
import org.jetbrains.compose.swing.foundation.layout.MeasureScope
import org.jetbrains.compose.swing.foundation.layout.OverlapLayout
import org.jetbrains.compose.swing.foundation.layout.Placeable
import org.jetbrains.compose.swing.foundation.layout.PlacementScope
import org.jetbrains.compose.swing.foundation.layout.TestPolicyLayout
import org.jetbrains.compose.swing.foundation.layout.peered
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.layout.LayoutElement
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * What a node declares to the layout manager of the parent holding it decides what that parent lays the
 * node out as, so a change to either declaration has to leave the parent waiting to be laid out again. A
 * parent that lays each child out at the size the child asks for reads no chain at all, so a chain
 * declared into one is refused rather than quietly dropped.
 *
 * These cases run under a frame that grants the tree a peer, since a container without one never counts
 * as laid out in the first place and so cannot be seen to stop counting as one.
 */
class ParentDeclarationTest {
    private val owners = mutableListOf<TestCompositionOwner>()

    @AfterTest
    fun disposeOwners() {
        owners.forEach { it.dispose() }
        owners.clear()
    }

    @Test
    fun aConstraintChangeRelaysTheParentOut() {
        val root = JPanel(BorderLayout())

        laidOut(root) { child ->
            child.declaration.applyConstraint(BorderLayout.SOUTH)

            assertFalse(root.isValid, "a child taking a new region must leave its parent to be laid out again")
        }
    }

    @Test
    fun aConstraintRedeclarationKeepsAMeasuredChildAndItsModifierChain() {
        val layout = TestPolicyLayout(SingleChildPolicy())
        val root = JPanel(layout)

        laidOut(root) { child ->
            val chain = paddingChain()
            child.declaration.applyLayoutChain(chain)
            root.validate()
            val measurable = layout.measurables.of(child.component)

            child.declaration.applyConstraint("replacement")

            assertSame(
                measurable,
                layout.measurables.of(child.component),
                "a MeasurePolicyLayout constraint redeclaration must not rebuild the child measurable",
            )
            assertSame(
                chain,
                measurable.layoutChain,
                "the child must keep the modifier chain it is measured through",
            )
        }
    }

    @Test
    fun aConstraintRedeclarationStillUsesAMeasurePolicyLayoutsConstraintValidation() {
        val root = JPanel(OverlapLayout(Alignment.TopStart))

        laidOut(root) { child ->
            assertFailsWith<IllegalArgumentException>(
                "a box must reject a redeclared constraint that belongs to another layout",
            ) {
                child.declaration.applyConstraint("not a BoxConstraint")
            }
        }
    }

    @Test
    fun aLayoutChainChangeRelaysTheParentOut() {
        val root = JPanel(TestPolicyLayout(SingleChildPolicy()))

        laidOut(root) { child ->
            child.declaration.applyLayoutChain(paddingChain())

            assertFalse(root.isValid, "a child measured through new layout modifiers must do the same")
        }
    }

    @Test
    fun aChainDeclaredIntoAParentThatMeasuresNothingIsRefused() {
        val root = JPanel(BorderLayout())

        laidOut(root) { child ->
            val refusal =
                assertFailsWith<IllegalStateException>(
                    "a child that comes to declare layout modifiers under a parent measuring none must be refused",
                ) {
                    child.declaration.applyLayoutChain(paddingChain())
                }

            assertEquals(
                hostCannotMeasureChild(root, child),
                refusal.message,
                "such a child must be given the account one arriving at that parent is given",
            )
        }
    }

    @Test
    fun anEmptyChainDeclaredIntoAParentThatMeasuresNothingStands() {
        val root = JPanel(BorderLayout())

        laidOut(root) { child ->
            child.declaration.applyLayoutChain(emptyList())

            assertTrue(
                child.declaration.layoutChain.isEmpty(),
                "a child declaring no layout modifiers must stand under a parent that measures none",
            )
        }
    }

    @Test
    fun aChainDeclaredByAChildALookAndFeelMovedStands() {
        val root = JPanel(TestPolicyLayout(SingleChildPolicy()))

        laidOut(root) { child ->
            // What a look and feel does of its own accord, as BasicToolBarUI does for a tool bar the
            // user drags out of the container the composition put it in.
            JPanel(BorderLayout()).add(child.component)

            child.declaration.applyLayoutChain(paddingChain())

            assertEquals(
                paddingChain(),
                child.declaration.layoutChain,
                "a child standing somewhere the composition did not put it must be held to the host that " +
                    "composition chose, which measures it, and not to the one it was moved into",
            )
        }
    }

    /**
     * Runs [assertion] over a child the applier attached to [root], with [root] laid out and holding a
     * peer - which is what a container has to hold to count as laid out at all.
     */
    private fun laidOut(
        root: JPanel,
        assertion: (SwingNodeHolder<*>) -> Unit,
    ) {
        val owner = TestCompositionOwner.observing()
        owners += owner
        val applier = SwingApplier(SwingNodeHolder(root).attachedTo(owner))
        val child = SwingNodeHolder(JButton("child"))

        peered(root) {
            applier.onBeginChanges()
            applier.down(applier.root)
            applier.insertBottomUp(0, child)
            applier.up()
            applier.onEndChanges()
            root.setSize(ROOT_EXTENT, ROOT_EXTENT)
            root.validate()
            assertTrue(root.isValid, "the parent must start out laid out, or nothing here is being asserted")

            assertion(child)
        }
    }

    /** The layout modifiers a padded child is measured through, as its container reads them. */
    private fun paddingChain(): List<LayoutElement> {
        val padded = with(BoxScopeImpl) { SwingModifier.padding(PADDING) }
        return padded.foldIn(mutableListOf()) { chain, element ->
            chain.also { if (element is LayoutElement) it.add(element) }
        }
    }

    private companion object {
        /** Large enough that the child is never held to less than it asks for. */
        const val ROOT_EXTENT = 200

        const val PADDING = 4
    }
}

/** A policy measuring its one child under the whole of what its container was offered. */
private class SingleChildPolicy :
    MeasurePolicy,
    MeasureResult {
    private lateinit var child: Placeable

    override val width: Int get() = child.width

    override val height: Int get() = child.height

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        child = measurables.single().measure(constraints)
        return this@SingleChildPolicy
    }

    override fun PlacementScope.placeChildren() {
        child.place(0, 0)
    }
}
