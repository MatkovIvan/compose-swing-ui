package org.jetbrains.compose.swing.modifier

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Canvas
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.modifier.accessibility.accessibleDescription
import org.jetbrains.compose.swing.modifier.accessibility.accessibleName
import org.jetbrains.compose.swing.modifier.accessibility.labelFor
import org.jetbrains.compose.swing.modifier.accessibility.labelTarget
import org.jetbrains.compose.swing.modifier.accessibility.mnemonic
import org.jetbrains.compose.swing.modifier.accessibility.rememberLabelTarget
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.SwingMatcher
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.Component
import java.awt.Dimension
import java.awt.event.KeyEvent
import javax.accessibility.AccessibleRole
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JTextField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Behavioral coverage for the accessibility modifiers. Each test reads back the applied state through
 * the live component's `AccessibleContext` or the Swing affordance the modifier wires (label
 * association, mnemonic), and asserts restoration to the pre-modifier default once an element leaves
 * the chain.
 */
class AccessibilityModifierTest {
    @Test
    fun accessibleNameAppliesAndRestoresOnRemoval() = runSwingUiTest {
        var named by mutableStateOf(true)
        setContent {
            TextField(
                "",
                modifier =
                    SwingModifier.testTag("field").let {
                        if (named) it.accessibleName("City field") else it
                    },
            )
        }
        // A JTextField has no intrinsic accessible name, so the default is null.
        assertEquals(
            "City field",
            onNodeWithTag("field").fetch<JTextField>().accessibleContext.accessibleName,
            "the accessible name should apply while the modifier is present",
        )

        named = false
        awaitIdle()
        // The element left the chain, restoring the pre-modifier default (null for a text field).
        assertNull(
            onNodeWithTag("field").fetch<JTextField>().accessibleContext.accessibleName,
            "removing the modifier should restore the null accessible name",
        )
    }

    @Test
    fun accessibleDescriptionAppliesAndRestoresOnRemoval() = runSwingUiTest {
        var described by mutableStateOf(true)
        setContent {
            Button(
                "Save",
                modifier =
                    SwingModifier.testTag("btn").let {
                        if (described) it.accessibleDescription("Persist the document") else it
                    },
            )
        }
        assertEquals(
            "Persist the document",
            onNodeWithTag("btn").fetch<JButton>().accessibleContext.accessibleDescription,
            "the accessible description should apply while the modifier is present",
        )

        described = false
        awaitIdle()
        assertNull(
            onNodeWithTag("btn").fetch<JButton>().accessibleContext.accessibleDescription,
            "removing the modifier should restore the null accessible description",
        )
    }

    @Test
    fun accessibleNameAndDescriptionAreFoundByMatchers() = runSwingUiTest {
        setContent {
            Label(
                "X",
                modifier =
                    SwingModifier
                        .testTag("lbl")
                        .accessibleName("Coordinate")
                        .accessibleDescription("The horizontal position"),
            )
        }
        onNode(SwingMatcher.hasAccessibleName("Coordinate")).assertExists()
        onNode(SwingMatcher.hasAccessibleDescription("The horizontal position")).assertExists()
    }

    @Test
    fun canvasReportsIntrinsicCanvasRole() = runSwingUiTest {
        setContent {
            Canvas(
                modifier =
                    SwingModifier
                        .testTag("surface")
                        .preferredSize(Dimension(40, 40)),
            ) { _, _, _ -> }
        }
        // A drawing surface reports CANVAS by construction; a plain JComponent would report the generic
        // SWING_COMPONENT role instead.
        assertEquals(
            AccessibleRole.CANVAS,
            onNodeWithTag("surface").fetch<Component>().accessibleContext.accessibleRole,
            "the canvas should report its intrinsic CANVAS role",
        )
        onNode(SwingMatcher.hasAccessibleRole(AccessibleRole.CANVAS)).assertExists()
    }

    @Test
    fun mnemonicOnButtonAppliesAndRestoresOnRemoval() = runSwingUiTest {
        var withMnemonic by mutableStateOf(true)
        setContent {
            Button(
                "Save",
                modifier =
                    SwingModifier.testTag("btn").let {
                        if (withMnemonic) it.mnemonic('S') else it
                    },
            )
        }
        assertEquals(
            KeyEvent.VK_S,
            onNodeWithTag("btn").fetch<JButton>().mnemonic,
            "the mnemonic should apply while present",
        )

        withMnemonic = false
        awaitIdle()
        assertEquals(
            0,
            onNodeWithTag("btn").fetch<JButton>().mnemonic,
            "removing the modifier should restore the zero mnemonic",
        )
    }

    @Test
    fun mnemonicOnLabelSetsDisplayedMnemonic() = runSwingUiTest {
        setContent {
            Label("Name", modifier = SwingModifier.testTag("lbl").mnemonic('N'))
        }
        assertEquals(KeyEvent.VK_N, onNodeWithTag("lbl").fetch<JLabel>().displayedMnemonic)
    }

    @Test
    fun labelForAssociatesLabelWithItsTarget() = runSwingUiTest {
        setContent {
            val usernameField = rememberLabelTarget()
            Label("Name", modifier = SwingModifier.labelFor(usernameField))
            // testTag identifies the field to the test harness; labelTarget wires the caption to it.
            TextField("", modifier = SwingModifier.labelTarget(usernameField).testTag("field"))
        }
        awaitIdle()
        val field = onNodeWithTag("field").fetch<JTextField>()
        assertSame(field, onNodeWithText("Name").fetch<JLabel>().labelFor)
    }

    @Test
    fun labelForResolvesRegardlessOfDeclarationOrder() = runSwingUiTest {
        setContent {
            val usernameField = rememberLabelTarget()
            // The target is declared before its label; the reference still pairs them once both attach.
            TextField("", modifier = SwingModifier.labelTarget(usernameField).testTag("field"))
            Label("Name", modifier = SwingModifier.labelFor(usernameField))
        }
        awaitIdle()
        assertSame(
            onNodeWithTag("field").fetch<JTextField>(),
            onNodeWithText("Name").fetch<JLabel>().labelFor,
        )
    }

    @Test
    fun checkBoxMnemonicApplies() = runSwingUiTest {
        setContent {
            CheckBox(modifier = SwingModifier.testTag("cb").mnemonic('A'), text = "Agree")
        }
        assertEquals(KeyEvent.VK_A, onNodeWithTag("cb").fetch<javax.swing.JCheckBox>().mnemonic)
    }
}
