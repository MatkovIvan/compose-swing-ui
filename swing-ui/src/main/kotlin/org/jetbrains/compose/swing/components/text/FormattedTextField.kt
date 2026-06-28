@file:JvmMultifileClass
@file:JvmName("TextComponentsKt")

package org.jetbrains.compose.swing.components.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.constants.FocusLostBehavior
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.propertyChangeListener
import java.beans.PropertyChangeListener
import javax.swing.JFormattedTextField
import javax.swing.JFormattedTextField.AbstractFormatterFactory

/**
 * A composable wrapper for `JFormattedTextField`, for number, date, or masked input.
 *
 * The field parses and formats through [formatterFactory], which produces the formatter that maps
 * between the typed [value] and the displayed text (e.g. a `NumberFormatter`, a `DateFormatter`, or a
 * `MaskFormatter` for a fixed mask). With no factory the field falls back to the platform default,
 * which formats by the value's type.
 *
 * [value] is the committed, typed value (an `Int`, a `Date`, a `String`, …); [onValueChange] fires
 * once per committed value, carrying the newly parsed value. A value the user types that does not
 * parse is not committed and produces no callback until it becomes valid. When [value] equals the
 * field's current committed value the set is skipped, so a callback that writes [value] back does not
 * loop.
 *
 * ```
 * FormattedTextField(
 *     value = amount,
 *     formatterFactory = DefaultFormatterFactory(NumberFormatter()),
 *     onValueChange = { amount = it as Int },
 * )
 * ```
 *
 * @param value the committed, typed value
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param formatterFactory the factory producing the field's formatter, or `null` for the default
 * @param onValueChange callback invoked when the committed value changes, with the parsed value
 * @param focusLostBehavior what to do with a partial edit when the field loses focus (a
 *   [FocusLostBehavior] `JFormattedTextField` constant)
 * @param columns the preferred width in columns; `0` sizes to the content
 */
@Composable
public fun FormattedTextField(
    value: Any?,
    modifier: SwingModifier = SwingModifier,
    formatterFactory: AbstractFormatterFactory? = null,
    onValueChange: (Any?) -> Unit = {},
    @FocusLostBehavior focusLostBehavior: Int = JFormattedTextField.COMMIT_OR_REVERT,
    columns: Int = 0,
) {
    val callback = rememberUpdatedState(onValueChange)
    val listener =
        remember { PropertyChangeListener { event -> callback.value((event.source as JFormattedTextField).value) } }
    FormattedTextField(
        value = value,
        valuePropertyChangeListener = listener,
        modifier = modifier,
        formatterFactory = formatterFactory,
        focusLostBehavior = focusLostBehavior,
        columns = columns,
    )
}

/**
 * A [FormattedTextField] driven by a raw [PropertyChangeListener] (bound to the `value` property)
 * instead of an `onValueChange` lambda. The listener is attached as-is and removed on the same
 * instance; pass a stable instance (e.g. `remember {}`) to avoid churn.
 *
 * @param value the committed, typed value
 * @param valuePropertyChangeListener the listener notified when the committed `value` changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param formatterFactory the factory producing the field's formatter, or `null` for the default
 * @param focusLostBehavior what to do with a partial edit when the field loses focus (a
 *   [FocusLostBehavior] `JFormattedTextField` constant)
 * @param columns the preferred width in columns; `0` sizes to the content
 */
@Composable
public fun FormattedTextField(
    value: Any?,
    valuePropertyChangeListener: PropertyChangeListener,
    modifier: SwingModifier = SwingModifier,
    formatterFactory: AbstractFormatterFactory? = null,
    @FocusLostBehavior focusLostBehavior: Int = JFormattedTextField.COMMIT_OR_REVERT,
    columns: Int = 0,
) {
    SwingNode(
        factory = { JFormattedTextField() },
        update = {
            set(columns) { this.columns = it }
            set(focusLostBehavior) { this.focusLostBehavior = it }
            set(formatterFactory) { this.setFormatterFactory(it) }
            set(value) { if (this.value != it) this.value = it }
            applyModifier(
                SwingModifier.propertyChangeListener("value", valuePropertyChangeListener) then modifier,
            )
        },
    )
}
