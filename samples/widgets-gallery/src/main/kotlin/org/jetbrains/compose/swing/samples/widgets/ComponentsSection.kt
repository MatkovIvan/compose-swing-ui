package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.ComboBox
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.ProgressBar
import org.jetbrains.compose.swing.components.Separator
import org.jetbrains.compose.swing.components.Slider
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.button.RadioButton
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.components.layout.ScrollPane
import org.jetbrains.compose.swing.components.selection.ListBox
import org.jetbrains.compose.swing.components.text.PasswordField
import org.jetbrains.compose.swing.components.text.TextArea
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.layout.alignmentX
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import java.awt.Dimension
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

/**
 * Demonstrates the leaf input/display components, each wired to `remember { mutableStateOf(...) }`
 * so the rendered value and the live state stay in lock-step.
 */
@Composable
internal fun ComponentsSection() {
    SectionColumn {
        SectionHeading("Components")
        ButtonAndLabelCard()
        TextInputCard()
        PasswordCard()
        ToggleCard()
        ChoiceCard()
        RangeCard()
        ListBoxCard()
        SeparatorCard()
    }
}

/** Button drives a counter; Label reflects it and shows the alignment options. */
@Composable
private fun ButtonAndLabelCard() {
    ExampleCard("Button & Label") {
        var counter by remember { mutableIntStateOf(0) }
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Label("Counter: $counter")
            Button("Increment", onClick = { counter++ })
            Button("Decrement", onClick = { counter-- })
            Button("Reset", onClick = { counter = 0 })
        }
        Label("Right-aligned label", horizontalAlignment = SwingConstants.RIGHT)
    }
}

/** TextField and TextArea share one backing value, so editing either updates the echo label. */
@Composable
private fun TextInputCard() {
    ExampleCard("TextField & TextArea") {
        var line by remember { mutableStateOf("Edit me") }
        var notes by remember { mutableStateOf("Multi-line\ntext area") }
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Label("TextField:")
            TextField(value = line, onValueChange = { line = it }, columns = 24)
        }
        Label("Echo: $line")
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Label("TextArea:")
            TextArea(value = notes, onValueChange = { notes = it }, rows = 3, columns = 30)
        }
    }
}

/**
 * PasswordField exposes its value as a [CharArray] (mirroring `getPassword()`); the strength echo is
 * derived from the array length without ever interning the cleartext into a String.
 */
@Composable
private fun PasswordCard() {
    ExampleCard("PasswordField (CharArray)") {
        var secret by remember { mutableStateOf(CharArray(0)) }
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Label("Password:")
            PasswordField(value = secret, onValueChange = { secret = it }, columns = 20)
        }
        Label("Length: ${secret.size}")
    }
}

/** CheckBox and a group of RadioButtons, each a controlled boolean/index. */
@Composable
private fun ToggleCard() {
    ExampleCard("CheckBox & RadioButton") {
        var checked by remember { mutableStateOf(false) }
        CheckBox(
            text = "Enable feature",
            checked = checked,
            onCheckedChange = { checked = it },
        )
        Label("Feature is ${if (checked) "on" else "off"}")

        var choice by remember { mutableIntStateOf(0) }
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            RadioButton(text = "Low", selected = choice == 0, onSelect = { choice = 0 })
            RadioButton(text = "Medium", selected = choice == 1, onSelect = { choice = 1 })
            RadioButton(text = "High", selected = choice == 2, onSelect = { choice = 2 })
        }
        Label("Priority index: $choice")
    }
}

/** ComboBox selection feeds the echo label. */
@Composable
private fun ChoiceCard() {
    ExampleCard("ComboBox") {
        val options = listOf("Kotlin", "Java", "Scala", "Groovy")
        var selected by remember { mutableIntStateOf(0) }
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Label("Language:")
            ComboBox(
                items = options,
                selectedIndex = selected,
                onSelectionChange = { selected = it },
                modifier = SwingModifier.testTag(LANGUAGE_COMBO_TAG),
            )
        }
        Label("Selected: ${options.getOrElse(selected) { "none" }}")
    }
}

/** Slider drives a value that an indeterminate-aware ProgressBar mirrors. */
@Composable
private fun RangeCard() {
    ExampleCard("Slider & ProgressBar") {
        var amount by remember { mutableIntStateOf(40) }
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Label("Amount: $amount")
            Slider(
                value = amount,
                onValueChange = { amount = it },
                min = 0,
                max = 100,
                modifier = SwingModifier.testTag(AMOUNT_SLIDER_TAG),
            )
        }
        ProgressBar(value = amount, min = 0, max = 100)
        Label("Indeterminate ProgressBar:")
        ProgressBar(indeterminate = true)
    }
}

/**
 * ListBox lives inside a [ScrollPane] (JList is not self-scrolling) and uses
 * [ListSelectionModel.MULTIPLE_INTERVAL_SELECTION] so the selection echo shows several indices at once.
 */
@Composable
private fun ListBoxCard() {
    ExampleCard("ListBox in a ScrollPane") {
        val rows = (1..30).map { "Row $it" }
        var selection by remember { mutableStateOf(listOf(0)) }
        ScrollPane(modifier = SwingModifier.preferredSize(Dimension(220, 120)).alignmentX(LEFT_ALIGNED)) {
            content {
                ListBox(
                    items = rows,
                    selectedIndices = selection,
                    onSelectionChange = { selection = it },
                    selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
                )
            }
        }
        Label("Selected indices: $selection")
    }
}

/** Separator in both orientations. */
@Composable
private fun SeparatorCard() {
    ExampleCard("Separator") {
        Label("Above the horizontal separator")
        Separator(orientation = SwingConstants.HORIZONTAL)
        Label("Below the horizontal separator")
    }
}

/** Test tags for the Components controls the showcase's behavioral tests drive directly. */
internal const val LANGUAGE_COMBO_TAG: String = "components-language-combo"
internal const val AMOUNT_SLIDER_TAG: String = "components-amount-slider"
