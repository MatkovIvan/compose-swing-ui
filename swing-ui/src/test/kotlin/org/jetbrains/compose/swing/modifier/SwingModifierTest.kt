package org.jetbrains.compose.swing.modifier

import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.modifier.appearance.background
import org.jetbrains.compose.swing.modifier.appearance.foreground
import org.jetbrains.compose.swing.modifier.appearance.name
import org.jetbrains.compose.swing.modifier.appearance.opaque
import org.jetbrains.compose.swing.modifier.interaction.enabled
import org.jetbrains.compose.swing.modifier.interaction.onHover
import org.jetbrains.compose.swing.modifier.layout.alignmentX
import org.jetbrains.compose.swing.modifier.layout.alignmentY
import org.jetbrains.compose.swing.modifier.layout.componentOrientation
import org.jetbrains.compose.swing.modifier.layout.maximumSize
import org.jetbrains.compose.swing.modifier.layout.minimumSize
import org.jetbrains.compose.swing.modifier.layout.visible
import org.jetbrains.compose.swing.modifier.listener.listener
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.Color
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton
import javax.swing.JComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Behavioral tests for [SwingModifier]: they assert what an observer of the live Swing component
 * sees — applied properties, reaction to recomposition, restoration when an element is removed, and
 * real listener behavior under dispatched events — never the internal diff/record machinery.
 */
class SwingModifierTest {
    private fun mouseEntered(component: Component): MouseEvent =
        MouseEvent(component, MouseEvent.MOUSE_ENTERED, 0L, 0, 0, 0, 0, false)

    private fun mouseEnterListener(onEnter: () -> Unit): MouseListener = object : MouseAdapter() {
        override fun mouseEntered(e: MouseEvent?): Unit = onEnter()
    }

    @Test
    fun nameModifierMakesComponentFindable() = runSwingUiTest {
        setContent { Button("Save", modifier = SwingModifier.name("save-button")) }
        onNodeWithName("save-button").assertExists()
    }

    @Test
    fun appearanceModifierAppliesAndReactsToState() = runSwingUiTest {
        var accent by mutableStateOf(true)
        setContent {
            Label(
                "X",
                modifier = SwingModifier.name("lbl").foreground(if (accent) Color.RED else Color.BLUE),
            )
        }
        assertEquals(
            Color.RED,
            onNodeWithName("lbl").fetch<JComponent>().foreground,
            "the modifier should apply the initial color",
        )

        accent = false
        awaitIdle()
        assertEquals(
            Color.BLUE,
            onNodeWithName("lbl").fetch<JComponent>().foreground,
            "the modifier should react to the state change",
        )
    }

    @Test
    fun multipleModifierElementsAllApply() = runSwingUiTest {
        setContent {
            Label(
                "X",
                modifier =
                    SwingModifier
                        .name("lbl")
                        .foreground(Color.RED)
                        .background(Color.BLUE)
                        .opaque(true),
            )
        }
        val label = onNodeWithName("lbl").fetch<JComponent>()
        assertEquals(Color.RED, label.foreground, "the foreground element should apply")
        assertEquals(Color.BLUE, label.background, "the background element should apply")
        assertTrue(label.isOpaque, "the opaque element should apply")
    }

    @Test
    fun removingAnElementRestoresThePriorDefault() = runSwingUiTest {
        var styled by mutableStateOf(true)
        setContent {
            Label("ctrl", modifier = SwingModifier.name("ctrl"))
            Label(
                "lbl",
                modifier = SwingModifier.name("lbl").let { if (styled) it.background(Color.YELLOW) else it },
            )
        }
        val default = onNodeWithName("ctrl").fetch<JComponent>().background
        assertEquals(
            Color.YELLOW,
            onNodeWithName("lbl").fetch<JComponent>().background,
            "the element should apply the background while present",
        )

        styled = false
        awaitIdle()
        // The element left the chain, so the background is restored to what it was before the
        // modifier first touched it (the same default the unstyled control still shows).
        assertEquals(
            default,
            onNodeWithName("lbl").fetch<JComponent>().background,
            "removing the element should restore the prior default",
        )
    }

    @Test
    fun hoverListenerFiresAndStopsAfterItsElementIsRemoved() = runSwingUiTest {
        var hoverEnabled by mutableStateOf(true)
        var enterCount = 0
        setContent {
            Button(
                "X",
                modifier =
                    SwingModifier.name("b").let {
                        if (hoverEnabled) it.onHover(onEnter = { enterCount++ }) else it
                    },
            )
        }
        val button = onNodeWithName("b").fetch<JComponent>()

        button.dispatchEvent(mouseEntered(button))
        assertEquals(1, enterCount, "the hover listener should fire while its element is present")

        hoverEnabled = false
        awaitIdle()
        button.dispatchEvent(mouseEntered(button))
        assertEquals(1, enterCount, "listener must be removed when its element leaves the chain")
    }

    @Test
    fun hoverListenerSeesTheLatestCallbackWithoutReinstalling() = runSwingUiTest {
        var target by mutableStateOf("first")
        var captured = ""
        setContent {
            Button("X", modifier = SwingModifier.name("b").onHover(onEnter = { captured = target }))
        }
        val button = onNodeWithName("b").fetch<JComponent>()

        button.dispatchEvent(mouseEntered(button))
        assertEquals("first", captured, "the hover listener should read the first callback")

        target = "second"
        awaitIdle()
        button.dispatchEvent(mouseEntered(button))
        assertEquals("second", captured, "the installed listener must read the latest callback")
    }

    @Test
    fun enabledModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        var disabled by mutableStateOf(true)
        setContent {
            Button(
                "X",
                modifier = SwingModifier.name("b").let { if (disabled) it.enabled(false) else it },
            )
        }
        assertFalse(
            onNodeWithName("b").fetch<JComponent>().isEnabled,
            "the modifier should disable the button while present",
        )

        disabled = false
        awaitIdle()
        // The element left the chain, so isEnabled is restored to the pre-modifier default.
        assertTrue(
            onNodeWithName("b").fetch<JComponent>().isEnabled,
            "removing the modifier should restore the enabled default",
        )
    }

    @Test
    fun visibleModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        var hidden by mutableStateOf(true)
        setContent {
            Button(
                "X",
                modifier = SwingModifier.name("b").let { if (hidden) it.visible(false) else it },
            )
        }
        assertFalse(
            onNodeWithName("b").fetch<JComponent>().isVisible,
            "the modifier should hide the button while present",
        )

        hidden = false
        awaitIdle()
        // The element left the chain, so isVisible is restored to the pre-modifier default.
        assertTrue(
            onNodeWithName("b").fetch<JComponent>().isVisible,
            "removing the modifier should restore the visible default",
        )
    }

    @Test
    fun minimumSizeModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        var constrained by mutableStateOf(true)
        setContent {
            Label(
                "X",
                modifier =
                    SwingModifier.name("lbl").let {
                        if (constrained) it.minimumSize(120, 40) else it
                    },
            )
        }
        assertEquals(
            Dimension(120, 40),
            onNodeWithName("lbl").fetch<JComponent>().minimumSize,
            "the modifier should apply the minimum size",
        )
        assertTrue(
            onNodeWithName("lbl").fetch<JComponent>().isMinimumSizeSet,
            "the minimum-size-set flag should be on while present",
        )

        constrained = false
        awaitIdle()
        // The element left the chain, so the explicit minimum size is cleared again.
        assertFalse(
            onNodeWithName("lbl").fetch<JComponent>().isMinimumSizeSet,
            "removing the modifier should clear the minimum-size-set flag",
        )
    }

    @Test
    fun maximumSizeModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        var constrained by mutableStateOf(true)
        setContent {
            Label(
                "X",
                modifier =
                    SwingModifier.name("lbl").let {
                        if (constrained) it.maximumSize(Dimension(200, 80)) else it
                    },
            )
        }
        assertEquals(
            Dimension(200, 80),
            onNodeWithName("lbl").fetch<JComponent>().maximumSize,
            "the modifier should apply the maximum size",
        )
        assertTrue(
            onNodeWithName("lbl").fetch<JComponent>().isMaximumSizeSet,
            "the maximum-size-set flag should be on while present",
        )

        constrained = false
        awaitIdle()
        // The element left the chain, so the explicit maximum size is cleared again.
        assertFalse(
            onNodeWithName("lbl").fetch<JComponent>().isMaximumSizeSet,
            "removing the modifier should clear the maximum-size-set flag",
        )
    }

    @Test
    fun maximumSizeWidthHeightOverloadAppliesTheDimension() = runSwingUiTest {
        setContent {
            Label("X", modifier = SwingModifier.name("lbl").maximumSize(200, 80))
        }
        assertEquals(
            Dimension(200, 80),
            onNodeWithName("lbl").fetch<JComponent>().maximumSize,
            "the overload should apply the dimension",
        )
        assertTrue(
            onNodeWithName("lbl").fetch<JComponent>().isMaximumSizeSet,
            "the overload should set the maximum-size-set flag",
        )
    }

    @Test
    fun alignmentXModifierAppliesTheSetValue() = runSwingUiTest {
        setContent {
            Label("X", modifier = SwingModifier.name("lbl").alignmentX(0.0f))
        }
        assertEquals(
            0.0f,
            onNodeWithName("lbl").fetch<JComponent>().alignmentX,
            "the modifier should set the x alignment",
        )
    }

    @Test
    fun alignmentYModifierAppliesTheSetValue() = runSwingUiTest {
        setContent {
            Label("X", modifier = SwingModifier.name("lbl").alignmentY(1.0f))
        }
        assertEquals(
            1.0f,
            onNodeWithName("lbl").fetch<JComponent>().alignmentY,
            "the modifier should set the y alignment",
        )
    }

    @Test
    fun componentOrientationModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        var rtl by mutableStateOf(true)
        setContent {
            Label("ctrl", modifier = SwingModifier.name("ctrl"))
            Label(
                "lbl",
                modifier =
                    SwingModifier.name("lbl").let {
                        if (rtl) it.componentOrientation(ComponentOrientation.RIGHT_TO_LEFT) else it
                    },
            )
        }
        val default = onNodeWithName("ctrl").fetch<JComponent>().componentOrientation
        val styled = onNodeWithName("lbl").fetch<JComponent>().componentOrientation
        assertEquals(ComponentOrientation.RIGHT_TO_LEFT, styled, "the modifier should apply the RTL orientation")

        rtl = false
        awaitIdle()
        // The element left the chain, so the orientation is restored to the pre-modifier default
        // (the same one the untouched control still shows).
        assertEquals(
            default,
            onNodeWithName("lbl").fetch<JComponent>().componentOrientation,
            "removing the modifier should restore the default orientation",
        )
    }

    @Test
    fun twoListenersOnOneComponentBothFire() = runSwingUiTest {
        var first = 0
        var second = 0
        setContent {
            Button(
                "X",
                modifier =
                    SwingModifier
                        .name("b")
                        .listener<JButton, MouseListener>(
                            mouseEnterListener { first++ },
                            { c, l -> c.addMouseListener(l) },
                            { c, l -> c.removeMouseListener(l) },
                        ).listener<JButton, MouseListener>(
                            mouseEnterListener { second++ },
                            { c, l -> c.addMouseListener(l) },
                            { c, l -> c.removeMouseListener(l) },
                        ),
            )
        }
        val button = onNodeWithName("b").fetch<JComponent>()

        button.dispatchEvent(mouseEntered(button))
        // Listeners are additive: neither slot replaces the other, so both fire.
        assertEquals(1, first, "the first listener should fire once")
        assertEquals(1, second, "the second listener should fire once")
    }

    @Test
    fun twoHoverModifiersBothFire() = runSwingUiTest {
        var first = 0
        var second = 0
        setContent {
            Button(
                "X",
                modifier =
                    SwingModifier
                        .name("b")
                        .onHover(onEnter = { first++ })
                        .onHover(onEnter = { second++ }),
            )
        }
        val button = onNodeWithName("b").fetch<JComponent>()

        button.dispatchEvent(mouseEntered(button))
        // Two onHover are additive (each its own slot), so both enter callbacks fire.
        assertEquals(1, first, "the first hover modifier should fire once")
        assertEquals(1, second, "the second hover modifier should fire once")
    }

    @Test
    fun repeatedPropertyElementStillLastWins() = runSwingUiTest {
        setContent {
            Label(
                "X",
                modifier =
                    SwingModifier
                        .name("lbl")
                        .background(Color.RED)
                        .background(Color.BLUE),
            )
        }
        // Property elements keep last-wins semantics: two backgrounds collapse, the later wins.
        assertEquals(Color.BLUE, onNodeWithName("lbl").fetch<JComponent>().background)
    }

    @Test
    fun removingOneAdditiveListenerLeavesTheOtherInstalled() = runSwingUiTest {
        var firstHoverEnabled by mutableStateOf(true)
        var first = 0
        var second = 0
        setContent {
            Button(
                "X",
                modifier =
                    SwingModifier
                        .name("b")
                        .let { if (firstHoverEnabled) it.onHover(onEnter = { first++ }) else it }
                        .onHover(onEnter = { second++ }),
            )
        }
        val button = onNodeWithName("b").fetch<JComponent>()

        button.dispatchEvent(mouseEntered(button))
        assertEquals(1, first, "the first listener should fire once before removal")
        assertEquals(1, second, "the second listener should fire once before removal")

        // Drop the first additive listener. Positional identity shifts the survivor, so it is
        // detached and reinstalled — but it stays live and keeps firing.
        firstHoverEnabled = false
        awaitIdle()
        button.dispatchEvent(mouseEntered(button))
        assertEquals(1, first, "the removed listener must not fire again")
        assertEquals(2, second, "the surviving listener must stay installed and keep firing")
    }

    @Test
    fun reuseDrainsBothPropertyAndAdditiveRecordsThenReinstalls() = runSwingUiTest {
        var active by mutableStateOf(true)
        var enterCount = 0
        setContent {
            ReusableContentHost(active = active) {
                Label(
                    "X",
                    modifier =
                        SwingModifier
                            .name("lbl")
                            .background(Color.GREEN)
                            .onHover(onEnter = { enterCount++ }),
                )
            }
        }
        assertEquals(
            Color.GREEN,
            onNodeWithName("lbl").fetch<JComponent>().background,
            "the property element should apply before reuse",
        )
        onNodeWithName("lbl").fetch<JComponent>().let { it.dispatchEvent(mouseEntered(it)) }
        assertEquals(1, enterCount, "the hover listener should fire once before reuse")

        // Deactivate then reactivate: resetModifierState drains both the keyed property record
        // (restoring the background) and the additive hover record (detaching the listener).
        active = false
        awaitIdle()
        active = true
        awaitIdle()

        // Property re-applied and listener re-installed on the reused node.
        assertEquals(
            Color.GREEN,
            onNodeWithName("lbl").fetch<JComponent>().background,
            "the property element must be re-applied after reuse",
        )
        onNodeWithName("lbl").fetch<JComponent>().let { it.dispatchEvent(mouseEntered(it)) }
        assertEquals(2, enterCount, "the additive listener must be re-installed after reuse")
    }

    @Test
    fun customElementWrapsAnArbitraryProperty() = runSwingUiTest {
        setContent { Button("X", modifier = SwingModifier.name("b").then(ToolTipElement("hello"))) }
        assertEquals("hello", onNodeWithName("b").fetch<JComponent>().toolTipText)
    }

    /**
     * A user-authored element proving the public [SwingModifier.Element] escape hatch works. It
     * targets [JComponent] via [targetType], so the node's `component` arrives already typed and the
     * body performs no cast. It captures the original tooltip in [Node.onAttach], writes the new value
     * in `update`, and restores the original in [Node.onDetach].
     */
    private class ToolTipElement(
        private val text: String,
    ) : SwingModifier.Element<JComponent, ToolTipElement.Node> {
        override val targetType: Class<JComponent> get() = JComponent::class.java

        override fun create(): Node = Node()

        override fun update(node: Node) {
            node.text = text
            node.apply()
        }

        class Node : SwingModifier.Node<JComponent>() {
            var text: String? = null
            private var original: String? = null

            override fun onAttach() {
                original = component.toolTipText
            }

            fun apply() {
                component.toolTipText = text
            }

            override fun onDetach() {
                component.toolTipText = original
            }
        }
    }
}
