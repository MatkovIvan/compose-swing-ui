package org.jetbrains.compose.swing.modifier

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Canvas
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.modifier.appearance.testTag
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
    fun accessibleRoleOverridesCanvasIntrinsicRole() = runSwingUiTest {
        var asImage by mutableStateOf(true)
        setContent {
            Canvas(
                modifier =
                    SwingModifier
                        .testTag("surface")
                        .preferredSize(Dimension(40, 40))
                        .let { if (asImage) it.accessibleRole(AccessibleRole.ICON) else it },
            ) { _, _, _ -> }
        }
        assertEquals(
            AccessibleRole.ICON,
            onNodeWithTag("surface").fetch<Component>().accessibleContext.accessibleRole,
            "the role override should replace the canvas intrinsic role",
        )
        onNode(SwingMatcher.hasAccessibleRole(AccessibleRole.ICON)).assertExists()

        asImage = false
        awaitIdle()
        // With no override the canvas reports its intrinsic CANVAS role.
        assertEquals(
            AccessibleRole.CANVAS,
            onNodeWithTag("surface").fetch<Component>().accessibleContext.accessibleRole,
            "removing the override should restore the intrinsic CANVAS role",
        )
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
    fun labelForAssociatesLabelWithTaggedTarget() = runSwingUiTest {
        setContent {
            Label("Name", modifier = SwingModifier.testTag("nameLabel").labelFor("nameField"))
            TextField("", modifier = SwingModifier.testTag("nameField"))
        }
        // The association is resolved via invokeLater; awaitIdle drains the EDT queue so it lands.
        awaitIdle()
        val field = onNodeWithTag("nameField").fetch<Component>()
        assertSame(field, onNodeWithTag("nameLabel").fetch<JLabel>().labelFor)
    }

    @Test
    fun checkBoxMnemonicApplies() = runSwingUiTest {
        setContent {
            CheckBox(modifier = SwingModifier.testTag("cb").mnemonic('A'), text = "Agree")
        }
        assertEquals(KeyEvent.VK_A, onNodeWithTag("cb").fetch<javax.swing.JCheckBox>().mnemonic)
    }
}
