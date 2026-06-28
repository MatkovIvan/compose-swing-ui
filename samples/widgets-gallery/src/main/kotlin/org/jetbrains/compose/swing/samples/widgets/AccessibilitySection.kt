package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Canvas
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.layout.BoxPanel
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.accessibleDescription
import org.jetbrains.compose.swing.modifier.accessibleName
import org.jetbrains.compose.swing.modifier.accessibleRole
import org.jetbrains.compose.swing.modifier.appearance.border
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.defaultButton
import org.jetbrains.compose.swing.modifier.focusTraversalIndex
import org.jetbrains.compose.swing.modifier.labelFor
import org.jetbrains.compose.swing.modifier.mnemonic
import org.jetbrains.compose.swing.modifier.orderedFocusTraversal
import org.jetbrains.compose.swing.modifier.preferredSize
import java.awt.Color
import java.awt.Dimension
import javax.accessibility.AccessibleRole
import javax.swing.BorderFactory
import javax.swing.BoxLayout

internal const val ACCESSIBLE_NAME_FIELD_TAG: String = "a11y-named-field"
internal const val LABEL_FOR_FIELD_TAG: String = "a11y-username-field"
internal const val ACCESSIBLE_ROLE_CANVAS_TAG: String = "a11y-role-canvas"

/**
 * Demonstrates the accessibility SwingModifiers: assistive-technology metadata
 * (accessibleName/accessibleDescription), label association (labelFor), keyboard affordances
 * (mnemonic, defaultButton), and composition-order focus traversal.
 */
@Composable
internal fun AccessibilitySection() {
    SectionColumn {
        SectionHeading("Accessibility")
        AccessibleMetadataCard()
        AccessibleRoleCard()
        LabelForCard()
        MnemonicCard()
        DefaultButtonCard()
        FocusTraversalCard()
    }
}

/** accessibleName and accessibleDescription advertise assistive-technology metadata on a field. */
@Composable
private fun AccessibleMetadataCard() {
    ExampleCard("accessibleName & accessibleDescription") {
        var value by remember { mutableStateOf("") }
        WrappedCaption("Screen readers announce the name + description below for this field.")
        TextField(
            value = value,
            modifier =
                SwingModifier
                    .testTag(ACCESSIBLE_NAME_FIELD_TAG)
                    .accessibleName("Search query")
                    .accessibleDescription("Type a term to filter the results list."),
            onValueChange = { value = it },
            columns = 24,
        )
    }
}

/** accessibleRole overrides the role a custom drawing surface reports to assistive technologies. */
@Composable
private fun AccessibleRoleCard() {
    ExampleCard("accessibleRole (Canvas reports as a slider)") {
        WrappedCaption("This drawing surface advertises the SLIDER role to assistive technologies.")
        Canvas(
            modifier =
                SwingModifier
                    .testTag(ACCESSIBLE_ROLE_CANVAS_TAG)
                    .preferredSize(Dimension(180, 40))
                    .border(BorderFactory.createLineBorder(Color.GRAY))
                    .accessibleRole(AccessibleRole.SLIDER),
        ) { g, width, height ->
            g.color = Color(0x42, 0x85, 0xF4)
            g.fillRect(0, height / 2 - 2, width / 2, 4)
        }
    }
}

/** labelFor wires a caption to the tagged field, so its mnemonic moves focus to the target. */
@Composable
private fun LabelForCard() {
    ExampleCard("labelFor (label captions a tagged field)") {
        var name by remember { mutableStateOf("") }
        FlowPanel {
            Label(
                "Username:",
                modifier = SwingModifier.labelFor(LABEL_FOR_FIELD_TAG).mnemonic('U'),
            )
            TextField(
                value = name,
                modifier = SwingModifier.testTag(LABEL_FOR_FIELD_TAG),
                onValueChange = { name = it },
                columns = 20,
            )
        }
    }
}

/** mnemonic underlines a letter that activates the button with the platform menu modifier. */
@Composable
private fun MnemonicCard() {
    ExampleCard("mnemonic (Alt+S activates Save)") {
        var saves by remember { mutableIntStateOf(0) }
        FlowPanel {
            Button(
                "Save",
                modifier = SwingModifier.mnemonic('S'),
                onClick = { saves++ },
            )
            Label("Saved $saves time(s)")
        }
    }
}

/** defaultButton makes a button the one Enter activates anywhere in the window. */
@Composable
private fun DefaultButtonCard() {
    ExampleCard("defaultButton (Enter activates Submit)") {
        var submits by remember { mutableIntStateOf(0) }
        FlowPanel {
            Button(
                "Submit",
                modifier = SwingModifier.defaultButton(true),
                onClick = { submits++ },
            )
            Label("Submitted $submits time(s)")
        }
    }
}

/** orderedFocusTraversal plus focusTraversalIndex tab through the form by composition-assigned order. */
@Composable
private fun FocusTraversalCard() {
    ExampleCard("focusTraversalIndex + orderedFocusTraversal (Tab order)") {
        var first by remember { mutableStateOf("") }
        var second by remember { mutableStateOf("") }
        var third by remember { mutableStateOf("") }
        WrappedCaption("Tab visits these fields bottom-to-top, following each field's index.")
        BoxPanel(
            modifier = SwingModifier.orderedFocusTraversal(),
            axis = BoxLayout.Y_AXIS,
        ) {
            TextField(
                value = third,
                modifier = SwingModifier.focusTraversalIndex(3),
                onValueChange = { third = it },
                columns = 20,
            )
            TextField(
                value = second,
                modifier = SwingModifier.focusTraversalIndex(2),
                onValueChange = { second = it },
                columns = 20,
            )
            TextField(
                value = first,
                modifier = SwingModifier.focusTraversalIndex(1),
                onValueChange = { first = it },
                columns = 20,
            )
        }
    }
}
