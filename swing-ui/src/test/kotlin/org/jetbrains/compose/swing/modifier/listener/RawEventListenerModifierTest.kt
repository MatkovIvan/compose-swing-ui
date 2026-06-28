package org.jetbrains.compose.swing.modifier.listener

import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.Slider
import org.jetbrains.compose.swing.components.layout.ScrollPane
import org.jetbrains.compose.swing.components.selection.ListBox
import org.jetbrains.compose.swing.components.selection.Tree
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.name
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.runSwingUiTest
import javax.swing.JInternalFrame
import javax.swing.JList
import javax.swing.JSlider
import javax.swing.JTree
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.InternalFrameAdapter
import javax.swing.event.InternalFrameEvent
import javax.swing.event.InternalFrameListener
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Behavioral tests for the raw event-listener builders that attach an existing Swing listener object to
 * a component whose add/remove pair lives off `java.awt.Component`: `changeListener`,
 * `listSelectionListener`, `treeSelectionListener`, and `internalFrameListener`. Each asserts what an
 * observer of the live component sees — the exact instance is registered via the component's
 * `getXxxListeners()`, and it fires when the component publishes the real event.
 */
class RawEventListenerModifierTest {
    @Test
    fun changeListenerInstanceIsRegisteredAndFiresOnAChange() = runSwingUiTest {
        var fired = 0
        val listener = ChangeListener { fired++ }
        setContent {
            Slider(value = 10, modifier = SwingModifier.name("s").changeListener(listener))
        }
        val slider = onNodeWithName("s").fetch<JSlider>()
        assertTrue(
            slider.changeListeners.any { it === listener },
            "the listener instance should be registered on the slider",
        )

        slider.changeListeners.forEach { it.stateChanged(ChangeEvent(slider)) }
        assertEquals(1, fired, "the registered listener should fire once on a change")
    }

    @Test
    fun listSelectionListenerInstanceIsRegisteredAndFiresOnSelection() = runSwingUiTest {
        var fired = 0
        val listener = ListSelectionListener { fired++ }
        setContent {
            ScrollPane {
                content {
                    ListBox(
                        items = listOf("a", "b", "c"),
                        modifier = SwingModifier.name("lst").listSelectionListener(listener),
                    )
                }
            }
        }
        val list = onNodeWithName("lst").fetch<JList<*>>()
        assertTrue(
            list.listSelectionListeners.any {
                it === listener
            },
            "the listener instance should be registered on the list",
        )

        list.selectedIndex = 1
        awaitIdle()
        assertTrue(fired > 0, "the registered selection listener must fire on a selection change")
    }

    @Test
    fun treeSelectionListenerInstanceIsRegisteredAndFiresOnSelection() = runSwingUiTest {
        var fired = 0
        val listener = TreeSelectionListener { fired++ }
        setContent {
            ScrollPane {
                content {
                    Tree(
                        root = "root",
                        children = { if (it == "root") listOf("child") else emptyList() },
                        modifier = SwingModifier.name("tree").treeSelectionListener(listener),
                    )
                }
            }
        }
        val tree = onNodeWithName("tree").fetch<JTree>()
        assertTrue(
            tree.treeSelectionListeners.any {
                it === listener
            },
            "the listener instance should be registered on the tree",
        )

        tree.setSelectionRow(0)
        awaitIdle()
        assertTrue(fired > 0, "the registered tree-selection listener must fire on a selection change")
    }

    @Test
    fun internalFrameListenerInstanceIsRegisteredAndFiresOnTheRealEvent() = runSwingUiTest {
        var closing = 0
        val listener: InternalFrameListener =
            object : InternalFrameAdapter() {
                override fun internalFrameClosing(event: InternalFrameEvent) {
                    closing++
                }
            }
        setContent {
            SwingNode(
                factory = { JInternalFrame("F", true, true, true, true).also { it.isVisible = true } },
                update = {
                    applyModifier(SwingModifier.name("frame").internalFrameListener(listener))
                },
            )
        }
        val frame = onNodeWithName("frame").fetch<JInternalFrame>()
        assertTrue(
            frame.internalFrameListeners.any {
                it === listener
            },
            "the listener instance should be registered on the frame",
        )

        val event = InternalFrameEvent(frame, InternalFrameEvent.INTERNAL_FRAME_CLOSING)
        frame.internalFrameListeners.forEach { it.internalFrameClosing(event) }
        assertEquals(1, closing, "the registered listener should fire once on the closing event")
    }

    @Test
    fun changeListenerOnAComponentThatDoesNotFireChangeEventsIsRejected() = runSwingUiTest {
        val error =
            assertFailsWith<IllegalStateException> {
                setContent {
                    // A Label is a JComponent but not one of the change-firing widgets, so the
                    // changeListener target is wrong and the applier must reject it loudly rather
                    // than silently no-op.
                    Label(
                        "X",
                        modifier =
                            SwingModifier.name("lbl").changeListener(ChangeListener { }),
                    )
                }
                awaitIdle()
            }
        val message = error.message.orEmpty()
        assertTrue(
            "fires change events" in message,
            "the wrong-target error must explain the required change-firing target, but was: $message",
        )
    }
}
