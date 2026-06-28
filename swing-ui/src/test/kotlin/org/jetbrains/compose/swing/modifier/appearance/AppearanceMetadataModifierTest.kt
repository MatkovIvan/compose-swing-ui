package org.jetbrains.compose.swing.modifier.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.interaction.focusable
import org.jetbrains.compose.swing.modifier.preferredSize
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.border.Border
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Behavioral coverage for the appearance and metadata [SwingModifier]s that lacked a dedicated suite:
 * font, border, cursor, toolTip, clientProperty, focusable and preferredSize. Each test asserts the
 * applied Swing property AND its restoration to the pre-modifier default once the element leaves the
 * chain — the round-trip contract every property element promises.
 */
class AppearanceMetadataModifierTest {
    @Test
    fun fontModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        val custom = Font("Monospaced", Font.BOLD, 22)
        var styled by mutableStateOf(true)
        setContent {
            Label("ctrl", modifier = SwingModifier.name("ctrl"))
            Label(
                "lbl",
                modifier = SwingModifier.name("lbl").let { if (styled) it.font(custom) else it },
            )
        }
        val default = onNodeWithName("ctrl").fetch<JComponent>().font
        assertEquals(
            custom,
            onNodeWithName("lbl").fetch<JComponent>().font,
            "the custom font should apply while present",
        )

        styled = false
        awaitIdle()
        // The element left the chain, so the font is restored to the pre-modifier default.
        assertEquals(
            default,
            onNodeWithName("lbl").fetch<JComponent>().font,
            "removing the modifier should restore the default font",
        )
    }

    @Test
    fun borderModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        val custom: Border = BorderFactory.createLineBorder(Color.RED, 3)
        var styled by mutableStateOf(true)
        setContent {
            Label("ctrl", modifier = SwingModifier.name("ctrl"))
            Label(
                "lbl",
                modifier = SwingModifier.name("lbl").let { if (styled) it.border(custom) else it },
            )
        }
        val default = onNodeWithName("ctrl").fetch<JComponent>().border
        assertSame(
            custom,
            onNodeWithName("lbl").fetch<JComponent>().border,
            "the custom border should apply while present",
        )

        styled = false
        awaitIdle()
        // The element left the chain, so the border is restored to the pre-modifier default (the
        // same one the untouched control still shows).
        assertSame(
            default,
            onNodeWithName("lbl").fetch<JComponent>().border,
            "removing the modifier should restore the default border",
        )
    }

    @Test
    fun cursorModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        val hand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        var styled by mutableStateOf(true)
        setContent {
            Label("ctrl", modifier = SwingModifier.name("ctrl"))
            Label(
                "lbl",
                modifier = SwingModifier.name("lbl").let { if (styled) it.cursor(hand) else it },
            )
        }
        val default = onNodeWithName("ctrl").fetch<JComponent>().cursor
        assertEquals(
            hand,
            onNodeWithName("lbl").fetch<JComponent>().cursor,
            "the custom cursor should apply while present",
        )

        styled = false
        awaitIdle()
        assertEquals(
            default,
            onNodeWithName("lbl").fetch<JComponent>().cursor,
            "removing the modifier should restore the default cursor",
        )
    }

    @Test
    fun toolTipModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        var styled by mutableStateOf(true)
        setContent {
            Label(
                "lbl",
                modifier = SwingModifier.name("lbl").let { if (styled) it.toolTip("hint") else it },
            )
        }
        assertEquals(
            "hint",
            onNodeWithName("lbl").fetch<JComponent>().toolTipText,
            "the tooltip should apply while present",
        )

        styled = false
        awaitIdle()
        // The element left the chain, so the tooltip is cleared back to the prior (null) default.
        assertNull(
            onNodeWithName("lbl").fetch<JComponent>().toolTipText,
            "removing the modifier should clear the tooltip",
        )
    }

    @Test
    fun clientPropertyModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        val key = "JComponent.sizeVariant"
        var styled by mutableStateOf(true)
        setContent {
            Label(
                "lbl",
                modifier =
                    SwingModifier.name("lbl").let {
                        if (styled) it.clientProperty(key, "small") else it
                    },
            )
        }
        assertEquals(
            "small",
            onNodeWithName("lbl").fetch<JComponent>().getClientProperty(key),
            "the client property should apply while present",
        )

        styled = false
        awaitIdle()
        // The element left the chain, so the client property is restored to its prior (null) value.
        assertNull(
            onNodeWithName("lbl").fetch<JComponent>().getClientProperty(key),
            "removing the modifier should clear the client property",
        )
    }

    @Test
    fun distinctClientPropertyKeysAreIndependentSlots() = runSwingUiTest {
        setContent {
            Label(
                "lbl",
                modifier =
                    SwingModifier
                        .name("lbl")
                        .clientProperty("k1", "v1")
                        .clientProperty("k2", "v2"),
            )
        }
        val label = onNodeWithName("lbl").fetch<JComponent>()
        assertEquals("v1", label.getClientProperty("k1"), "key k1 should hold its own value")
        assertEquals("v2", label.getClientProperty("k2"), "key k2 should hold its own value")
    }

    @Test
    fun focusableModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        var styled by mutableStateOf(true)
        setContent {
            Label("ctrl", modifier = SwingModifier.name("ctrl"))
            Label(
                "lbl",
                modifier = SwingModifier.name("lbl").let { if (styled) it.focusable(true) else it },
            )
        }
        // A JLabel is not focusable by default; the modifier flips it on.
        val default = onNodeWithName("ctrl").fetch<JComponent>().isFocusable
        assertTrue(
            onNodeWithName("lbl").fetch<JComponent>().isFocusable,
            "the modifier should make the label focusable",
        )

        styled = false
        awaitIdle()
        // The element left the chain, so isFocusable is restored to the untouched control's default.
        assertEquals(
            default,
            onNodeWithName("lbl").fetch<JComponent>().isFocusable,
            "removing the modifier should restore the default focusability",
        )
    }

    @Test
    fun preferredSizeModifierAppliesAndRestoresOnRemoval() = runSwingUiTest {
        var sized by mutableStateOf(true)
        setContent {
            Label(
                "lbl",
                modifier =
                    SwingModifier.name("lbl").let {
                        if (sized) it.preferredSize(Dimension(123, 45)) else it
                    },
            )
        }
        assertEquals(
            Dimension(123, 45),
            onNodeWithName("lbl").fetch<JComponent>().preferredSize,
            "the preferred size should apply while present",
        )
        assertTrue(
            onNodeWithName("lbl").fetch<JComponent>().isPreferredSizeSet,
            "the preferred-size-set flag should be on while present",
        )

        sized = false
        awaitIdle()
        // The element left the chain, so the explicit preferred size is cleared again.
        assertFalse(
            onNodeWithName("lbl").fetch<JComponent>().isPreferredSizeSet,
            "removing the modifier should clear the preferred-size-set flag",
        )
    }

    @Test
    fun preferredSizeWidthHeightOverloadAppliesTheDimension() = runSwingUiTest {
        setContent {
            Label("lbl", modifier = SwingModifier.name("lbl").preferredSize(123, 45))
        }
        assertEquals(
            Dimension(123, 45),
            onNodeWithName("lbl").fetch<JComponent>().preferredSize,
            "the overload should apply the dimension",
        )
        assertTrue(
            onNodeWithName("lbl").fetch<JComponent>().isPreferredSizeSet,
            "the overload should set the preferred-size-set flag",
        )
    }
}
