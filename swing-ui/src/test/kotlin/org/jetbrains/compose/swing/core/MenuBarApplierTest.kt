package org.jetbrains.compose.swing.core

import java.awt.Component
import javax.swing.JComponent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Low-level unit tests that drive [MenuBarApplier] directly over a root [JMenuBar], with no Compose
 * runtime, recomposer, or clock involved — the menu counterpart of `SwingApplierTest`. Now that the
 * menu applier operates on [SwingNodeHolder] wrappers (so menu nodes get the same lifecycle
 * callbacks as ordinary components), these tests pin its AWT-tree manipulation math: that children
 * are added to the right menu container (a `JMenu` routes to its popup), in composition order, and
 * that remove/move/clear address the AWT array by index correctly.
 *
 * All AWT work happens on the calling thread, which is acceptable for unit tests of pure tree
 * manipulation (no EDT-bound timers or compositions are created here).
 */
class MenuBarApplierTest {
    private fun holder(component: Component): SwingNodeHolder<*> = SwingNodeHolder(component)

    /** Positions `current` on [node], runs [block], and returns to the previous node. */
    private fun MenuBarApplier.onNode(
        node: SwingNodeHolder<*>,
        block: MenuBarApplier.() -> Unit,
    ) {
        down(node)
        block()
        up()
    }

    private fun namedItem(name: String): JMenuItem = JMenuItem(name).apply { this.name = name }

    private fun itemNames(menu: JMenu): List<String?> = (0 until menu.itemCount).map { menu.getItem(it)?.name }

    /** Observers created for the appliers under test, disposed in [disposeObservers]. */
    private val observers = mutableListOf<SwingSnapshotObserver>()

    /**
     * Builds a [MenuBarApplier] over [root] with a snapshot observer this test owns and disposes, so
     * the global apply-observer registration the applier starts is torn down at test end rather than
     * leaked (the production path disposes it with the composition mount).
     */
    private fun applierFor(root: JComponent): MenuBarApplier {
        val observer = SwingSnapshotObserver().apply { start() }
        observers += observer
        return MenuBarApplier(root, observer)
    }

    @AfterTest
    fun disposeObservers() {
        observers.forEach { it.dispose() }
        observers.clear()
    }

    @Test
    fun insertBottomUp_addsMenuToBar() {
        val bar = JMenuBar()
        val applier = applierFor(bar)
        val menu = JMenu("File")

        applier.onBeginChanges()
        applier.onNode(applier.root) {
            insertBottomUp(0, holder(menu))
        }
        applier.onEndChanges()

        assertEquals(1, bar.menuCount, "the bar should hold exactly the one inserted menu")
        assertSame(menu, bar.getMenu(0), "the inserted menu instance should be at index 0")
    }

    @Test
    fun insertBottomUp_routesItemsIntoMenuPopupInCompositionOrder() {
        val bar = JMenuBar()
        val applier = applierFor(bar)
        val menu = JMenu("File")
        val menuHolder = holder(menu)

        applier.onBeginChanges()
        applier.onNode(applier.root) { insertBottomUp(0, menuHolder) }
        applier.onNode(menuHolder) {
            insertBottomUp(0, holder(namedItem("a")))
            insertBottomUp(1, holder(namedItem("b")))
            // Insert "c" between a and b.
            insertBottomUp(1, holder(namedItem("c")))
        }
        applier.onEndChanges()

        assertEquals(listOf("a", "c", "b"), itemNames(menu))
    }

    @Test
    fun remove_dropsItemsByIndex() {
        val bar = JMenuBar()
        val applier = applierFor(bar)
        val menu = JMenu("File")
        val menuHolder = holder(menu)

        applier.onBeginChanges()
        applier.onNode(applier.root) { insertBottomUp(0, menuHolder) }
        applier.onNode(menuHolder) {
            insertBottomUp(0, holder(namedItem("a")))
            insertBottomUp(1, holder(namedItem("b")))
            insertBottomUp(2, holder(namedItem("c")))
            remove(1, 1)
        }
        applier.onEndChanges()

        assertEquals(listOf("a", "c"), itemNames(menu))
    }

    @Test
    fun move_reordersItems() {
        val bar = JMenuBar()
        val applier = applierFor(bar)
        val menu = JMenu("File")
        val menuHolder = holder(menu)

        applier.onBeginChanges()
        applier.onNode(applier.root) { insertBottomUp(0, menuHolder) }
        applier.onNode(menuHolder) {
            insertBottomUp(0, holder(namedItem("a")))
            insertBottomUp(1, holder(namedItem("b")))
            insertBottomUp(2, holder(namedItem("c")))
            // Move the last item to the front.
            move(2, 0, 1)
        }
        applier.onEndChanges()

        assertEquals(listOf("c", "a", "b"), itemNames(menu))
    }

    @Test
    fun onClear_removesAllMenus() {
        val bar = JMenuBar()
        val applier = applierFor(bar)

        applier.onBeginChanges()
        applier.onNode(applier.root) {
            insertBottomUp(0, holder(JMenu("File")))
            insertBottomUp(1, holder(JMenu("Edit")))
        }
        applier.onEndChanges()
        assertEquals(2, bar.menuCount, "the bar should hold both menus before clearing")

        applier.onBeginChanges()
        applier.clear()
        applier.onEndChanges()

        assertEquals(0, bar.menuCount, "clear should remove every menu from the bar")
    }
}
