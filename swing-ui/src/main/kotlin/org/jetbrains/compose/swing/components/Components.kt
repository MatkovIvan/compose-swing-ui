package org.jetbrains.compose.swing.components

import androidx.compose.runtime.*
import org.jetbrains.compose.swing.SwingNode
import java.awt.Component
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

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
    SwingNode(
        factory = { JButton() },
        update = {
            set(text) { this.text = it }
            set(enabled) { this.isEnabled = it }
            set(onClick) {
                this.actionListeners.forEach { this.removeActionListener(it) }
                this.addActionListener { onClick() }
            }
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
    SwingNode(
        factory = { JLabel() },
        update = {
            set(text) { this.text = it }
            set(horizontalAlignment) { this.horizontalAlignment = it }
        }
    )
}

/**
 * Private helper for managing text synchronization and document listener for JTextComponent.
 * Handles the common logic shared by TextField and TextArea.
 *
 * @param textComponent the JTextComponent instance (JTextField or JTextArea)
 * @param onValueChange callback invoked when the text changes
 */
@Composable
private fun TextComponentListener(
    textComponent: JTextComponent,
    onValueChange: (String) -> Unit
) {
    DisposableEffect(onValueChange) {
        val listener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                onValueChange(textComponent.text)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                onValueChange(textComponent.text)
            }

            override fun changedUpdate(e: DocumentEvent?) {
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

    SwingNode(
        factory = { textField },
        update = {
            set(enabled) { this.isEnabled = it }
            set(value) { if (this.text != it) this.text = it }
        }
    )

    TextComponentListener(textField, onValueChange)
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

    SwingNode(
        factory = { textArea },
        update = {
            set(enabled) { textArea.isEnabled = it }
            set(value) { if (textArea.text != it) textArea.text = it }
        }
    )

    TextComponentListener(textArea, onValueChange)
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
    SwingNode(
        factory = { JCheckBox() },
        update = {
            set(text) { this.text = it }
            set(checked) { this.isSelected = it }
            set(enabled) { this.isEnabled = it }
            set(onCheckedChange) {
                this.actionListeners.forEach { this.removeActionListener(it) }
                this.addActionListener { onCheckedChange(this.isSelected) }
            }
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
    SwingNode(
        factory = { JRadioButton() },
        update = {
            set(text) { this.text = it }
            set(selected) { this.isSelected = it }
            set(enabled) { this.isEnabled = it }
            set(onSelect) {
                this.actionListeners.forEach { this.removeActionListener(it) }
                this.addActionListener { if (this.isSelected) onSelect() }
            }
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
    SwingNode(
        factory = { JComboBox<T>() },
        update = {
            set(items) {
                this.removeAllItems()
                it.forEach { item -> this.addItem(item) }
            }
            set(selectedIndex) {
                if (it >= 0 && it < this.itemCount) {
                    this.selectedIndex = it
                }
            }
            set(enabled) { this.isEnabled = it }
            set(onSelectionChange) {
                this.actionListeners.forEach { this.removeActionListener(it) }
                this.addActionListener { onSelectionChange(this.selectedIndex) }
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
    SwingNode(
        factory = { JSlider(min, max, value) },
        update = {
            set(min) { this.minimum = it }
            set(max) { this.maximum = it }
            set(value) { this.value = it }
            set(enabled) { this.isEnabled = it }
            set(onValueChange) {
                this.changeListeners.forEach { this.removeChangeListener(it) }
                this.addChangeListener { onValueChange(this.value) }
            }
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
    SwingNode(
        factory = { JProgressBar(min, max) },
        update = {
            set(min) { this.minimum = it }
            set(max) { this.maximum = it }
            set(value) { this.value = it }
            set(indeterminate) { this.isIndeterminate = it }
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
    SwingNode(
        factory = { JSeparator(orientation) },
        update = {
            set(orientation) { this.orientation = it }
        }
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
    SwingNode(
        factory = {
            val panel = JPanel()
            JScrollPane(panel).apply {
                preferredSize?.let { this.preferredSize = it }
            }
        },
        update = {
            set(preferredSize) { it?.let { this.preferredSize = it } }
        }
    ) {
        content()
    }
}
