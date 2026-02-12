package org.jetbrains.compose.swing.components

import androidx.compose.runtime.*
import org.jetbrains.compose.swing.util.UpdateEffect
import java.awt.Component
import java.awt.Dimension
import javax.swing.*
import javax.swing.text.JTextComponent

/**
 * Base composable node for Swing components.
 * Manages the lifecycle and updates of a Swing component in the Compose tree.
 */
@Composable
internal fun <T : Component> SwingComponent(
    factory: () -> T,
    update: T.() -> Unit = {}
) {
    val component = remember { factory() }

    UpdateEffect {
        component.update()
    }

    ComposeNode<Component, Applier<Any>>(
        factory = { component },
        update = {}
    )
}

/**
 * A composable wrapper for JButton.
 *
 * @param text the text to display on the button
 * @param enabled whether the button is enabled
 * @param onClick callback to be invoked when the button is clicked
 */
@Composable
fun Button(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    SwingComponent(
        factory = { JButton() },
        update = {
            this.text = text
            this.isEnabled = enabled

            // Remove all existing action listeners and add the new one
            actionListeners.forEach { removeActionListener(it) }
            addActionListener { onClick() }
        }
    )
}

/**
 * A composable wrapper for JLabel.
 *
 * @param text the text to display
 * @param horizontalAlignment the horizontal alignment of the text
 */
@Composable
fun Label(
    text: String,
    horizontalAlignment: Int = SwingConstants.LEFT
) {
    SwingComponent(
        factory = { JLabel() },
        update = {
            this.text = text
            this.horizontalAlignment = horizontalAlignment
        }
    )
}

/**
 * Private helper for managing text synchronization and document listener for JTextComponent.
 * Handles the common logic shared by TextField and TextArea.
 *
 * @param textComponent the JTextComponent instance (JTextField or JTextArea)
 * @param value the current text value
 * @param onValueChange callback invoked when the text changes
 */
@Composable
private fun TextComponentEffects(
    textComponent: JTextComponent,
    value: String,
    onValueChange: (String) -> Unit
) {
    UpdateEffect {
        // Update text only if it differs from current text
        if (textComponent.text != value) {
            textComponent.text = value
        }
    }

    DisposableEffect(onValueChange) {
        val listener = object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) {
                onValueChange(textComponent.text)
            }

            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) {
                onValueChange(textComponent.text)
            }

            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) {
                onValueChange(textComponent.text)
            }
        }
        textComponent.document.addDocumentListener(listener)

        onDispose {
            textComponent.document.removeDocumentListener(listener)
        }
    }
}

/**
 * A composable wrapper for JTextField.
 *
 * @param value the current text value
 * @param onValueChange callback invoked when the text changes
 * @param enabled whether the text field is enabled
 * @param columns the number of columns
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit = {},
    enabled: Boolean = true,
    columns: Int = 20
) {
    val textField = remember { JTextField(columns) }

    ComposeNode<Component, Applier<Any>>(
        factory = { textField },
        update = {
            set(enabled) { textField.isEnabled = it }
        }
    )

    TextComponentEffects(textField, value, onValueChange)
}

/**
 * A composable wrapper for JTextArea.
 *
 * @param value the current text value
 * @param onValueChange callback invoked when the text changes
 * @param enabled whether the text area is enabled
 * @param rows the number of rows
 * @param columns the number of columns
 */
@Composable
fun TextArea(
    value: String,
    onValueChange: (String) -> Unit = {},
    enabled: Boolean = true,
    rows: Int = 5,
    columns: Int = 20
) {
    val textArea = remember { JTextArea(rows, columns) }

    ComposeNode<Component, Applier<Any>>(
        factory = { textArea },
        update = {
            set(enabled) { textArea.isEnabled = it }
        }
    )

    TextComponentEffects(textArea, value, onValueChange)
}

/**
 * A composable wrapper for JCheckBox.
 *
 * @param text the text to display next to the checkbox
 * @param checked whether the checkbox is checked
 * @param onCheckedChange callback invoked when the checked state changes
 * @param enabled whether the checkbox is enabled
 */
@Composable
fun CheckBox(
    text: String = "",
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    enabled: Boolean = true
) {
    SwingComponent(
        factory = { JCheckBox() },
        update = {
            this.text = text
            this.isSelected = checked
            this.isEnabled = enabled

            // Remove all existing action listeners and add the new one
            actionListeners.forEach { removeActionListener(it) }
            addActionListener { onCheckedChange(isSelected) }
        }
    )
}

/**
 * A composable wrapper for JRadioButton.
 *
 * @param text the text to display next to the radio button
 * @param selected whether the radio button is selected
 * @param onSelect callback invoked when the radio button is selected
 * @param enabled whether the radio button is enabled
 */
@Composable
fun RadioButton(
    text: String = "",
    selected: Boolean = false,
    onSelect: () -> Unit = {},
    enabled: Boolean = true
) {
    SwingComponent(
        factory = { JRadioButton() },
        update = {
            this.text = text
            this.isSelected = selected
            this.isEnabled = enabled

            // Remove all existing action listeners and add the new one
            actionListeners.forEach { removeActionListener(it) }
            addActionListener { if (isSelected) onSelect() }
        }
    )
}

/**
 * A composable wrapper for JComboBox.
 *
 * @param items the list of items to display
 * @param selectedIndex the index of the selected item
 * @param onSelectionChange callback invoked when the selection changes
 * @param enabled whether the combo box is enabled
 */
@Composable
fun <T> ComboBox(
    items: List<T>,
    selectedIndex: Int = -1,
    onSelectionChange: (Int) -> Unit = {},
    enabled: Boolean = true
) {
    SwingComponent(
        factory = { JComboBox<T>() },
        update = {
            removeAllItems()
            items.forEach { addItem(it) }
            if (selectedIndex >= 0 && selectedIndex < items.size) {
                this.selectedIndex = selectedIndex
            }
            this.isEnabled = enabled

            // Remove all existing action listeners and add the new one
            actionListeners.forEach { removeActionListener(it) }
            addActionListener {
                onSelectionChange(this.selectedIndex)
            }
        }
    )
}

/**
 * A composable wrapper for JSlider.
 *
 * @param value the current value
 * @param onValueChange callback invoked when the value changes
 * @param min the minimum value
 * @param max the maximum value
 * @param enabled whether the slider is enabled
 */
@Composable
fun Slider(
    value: Int,
    onValueChange: (Int) -> Unit = {},
    min: Int = 0,
    max: Int = 100,
    enabled: Boolean = true
) {
    SwingComponent(
        factory = { JSlider(min, max, value) },
        update = {
            this.minimum = min
            this.maximum = max
            this.value = value
            this.isEnabled = enabled

            // Remove all existing change listeners and add the new one
            changeListeners.forEach { removeChangeListener(it) }
            addChangeListener { onValueChange(this.value) }
        }
    )
}

/**
 * A composable wrapper for JProgressBar.
 *
 * @param value the current value
 * @param min the minimum value
 * @param max the maximum value
 * @param indeterminate whether the progress bar is indeterminate
 */
@Composable
fun ProgressBar(
    value: Int = 0,
    min: Int = 0,
    max: Int = 100,
    indeterminate: Boolean = false
) {
    SwingComponent(
        factory = { JProgressBar(min, max) },
        update = {
            this.minimum = min
            this.maximum = max
            this.value = value
            this.isIndeterminate = indeterminate
        }
    )
}

/**
 * A composable wrapper for JSeparator.
 *
 * @param orientation the orientation (SwingConstants.HORIZONTAL or SwingConstants.VERTICAL)
 */
@Composable
fun Separator(
    orientation: Int = SwingConstants.HORIZONTAL
) {
    SwingComponent(
        factory = { JSeparator(orientation) }
    )
}

/**
 * A composable wrapper for JScrollPane.
 *
 * @param preferredSize the preferred size of the scroll pane
 * @param content the composable content to be scrolled
 */
@Composable
fun ScrollPane(
    preferredSize: Dimension? = null,
    content: @Composable () -> Unit
) {
    SwingComponent(
        factory = {
            val panel = JPanel()
            JScrollPane(panel).apply {
                preferredSize?.let { this.preferredSize = it }
            }
        },
        update = {
            preferredSize?.let { this.preferredSize = it }
        }
    )
    content()
}
