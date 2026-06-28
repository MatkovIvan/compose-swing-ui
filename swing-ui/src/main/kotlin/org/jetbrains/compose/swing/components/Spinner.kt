@file:JvmMultifileClass
@file:JvmName("ComponentsKt")

package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.changeListener
import javax.swing.JSpinner
import javax.swing.SpinnerListModel
import javax.swing.SpinnerNumberModel
import javax.swing.event.ChangeListener

/**
 * A composable wrapper for `JSpinner` backed by an integer number model.
 *
 * The value is controlled via [value] + [onValueChange], stepping between [min] and [max] by [step].
 * A value the caller pushes in is applied without echoing back through [onValueChange]; only a change
 * originating from the spinner (arrow buttons, typing, scrolling) fires the callback.
 *
 * ```
 * Spinner(value = count, min = 0, max = 10, onValueChange = { count = it })
 * ```
 *
 * @param value the current value (controlled)
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param onValueChange callback invoked when the spinner's value changes
 * @param min the smallest selectable value
 * @param max the largest selectable value
 * @param step the increment between adjacent values
 */
@Composable
public fun Spinner(
    value: Int,
    modifier: SwingModifier = SwingModifier,
    onValueChange: (Int) -> Unit = {},
    min: Int = 0,
    max: Int = 100,
    step: Int = 1,
) {
    val callback = rememberUpdatedState(onValueChange)
    val listener = remember { ChangeListener { event -> callback.value((event.source as JSpinner).valueAs<Int>()) } }
    Spinner(value = value, changeListener = listener, modifier = modifier, min = min, max = max, step = step)
}

/**
 * An integer `JSpinner` driven by a raw [ChangeListener] instead of an `onValueChange` lambda. The
 * [changeListener] is attached as-is and removed on the same instance; pass a stable instance (e.g.
 * `remember {}`) to avoid churn.
 *
 * @param value the current value (controlled)
 * @param changeListener the listener notified when the spinner's value changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param min the smallest selectable value
 * @param max the largest selectable value
 * @param step the increment between adjacent values
 */
@Composable
public fun Spinner(
    value: Int,
    changeListener: ChangeListener,
    modifier: SwingModifier = SwingModifier,
    min: Int = 0,
    max: Int = 100,
    step: Int = 1,
) {
    SwingNode(
        factory = { JSpinner(SpinnerNumberModel(value, min, max, step)) },
        update = {
            set(min) { numberModel(this).minimum = it }
            set(max) { numberModel(this).maximum = it }
            set(step) { numberModel(this).stepSize = it }
            set(value) { applyValue(this, it) }
            applyModifier(SwingModifier.changeListener(changeListener) then modifier)
        },
    )
}

/**
 * A composable wrapper for `JSpinner` backed by a floating-point number model.
 *
 * The value is controlled via [value] + [onValueChange], stepping between [min] and [max] by [step].
 * A value the caller pushes in is applied without echoing back through [onValueChange]; only a change
 * originating from the spinner (arrow buttons, typing, scrolling) fires the callback.
 *
 * @param value the current value (controlled)
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param onValueChange callback invoked when the spinner's value changes
 * @param min the smallest selectable value
 * @param max the largest selectable value
 * @param step the increment between adjacent values
 */
@Composable
public fun Spinner(
    value: Double,
    modifier: SwingModifier = SwingModifier,
    onValueChange: (Double) -> Unit = {},
    min: Double = 0.0,
    max: Double = 100.0,
    step: Double = 1.0,
) {
    val callback = rememberUpdatedState(onValueChange)
    val listener = remember { ChangeListener { event -> callback.value((event.source as JSpinner).valueAs<Double>()) } }
    Spinner(value = value, changeListener = listener, modifier = modifier, min = min, max = max, step = step)
}

/**
 * A floating-point `JSpinner` driven by a raw [ChangeListener] instead of an `onValueChange` lambda.
 * The [changeListener] is attached as-is and removed on the same instance; pass a stable instance (e.g.
 * `remember {}`) to avoid churn.
 *
 * @param value the current value (controlled)
 * @param changeListener the listener notified when the spinner's value changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param min the smallest selectable value
 * @param max the largest selectable value
 * @param step the increment between adjacent values
 */
@Composable
public fun Spinner(
    value: Double,
    changeListener: ChangeListener,
    modifier: SwingModifier = SwingModifier,
    min: Double = 0.0,
    max: Double = 100.0,
    step: Double = 1.0,
) {
    SwingNode(
        factory = { JSpinner(SpinnerNumberModel(value, min, max, step)) },
        update = {
            set(min) { numberModel(this).minimum = it }
            set(max) { numberModel(this).maximum = it }
            set(step) { numberModel(this).stepSize = it }
            set(value) { applyValue(this, it) }
            applyModifier(SwingModifier.changeListener(changeListener) then modifier)
        },
    )
}

/**
 * A composable wrapper for `JSpinner` that cycles through a fixed list of [items].
 *
 * The selection is controlled via [selectedIndex] + [onSelectionChange]; the spinner shows the item at
 * that index and the arrow buttons move to the neighbouring item. A selection the caller pushes in is
 * applied without echoing back through [onSelectionChange]; only a change originating from the spinner
 * fires the callback.
 *
 * @param items the values the spinner cycles through
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedIndex the index of the currently shown item (controlled)
 * @param onSelectionChange callback invoked with the index of the newly shown item
 */
@Composable
public fun <T> Spinner(
    items: List<T>,
    modifier: SwingModifier = SwingModifier,
    selectedIndex: Int = 0,
    onSelectionChange: (Int) -> Unit = {},
) {
    val callback = rememberUpdatedState(onSelectionChange)
    val listener =
        remember {
            ChangeListener { event ->
                val spinner = event.source as JSpinner
                callback.value(listModel(spinner).list.indexOf(spinner.value))
            }
        }
    Spinner(items = items, changeListener = listener, modifier = modifier, selectedIndex = selectedIndex)
}

/**
 * A list `JSpinner` driven by a raw [ChangeListener] instead of an `onSelectionChange` lambda. The
 * [changeListener] is attached as-is and removed on the same instance; pass a stable instance (e.g.
 * `remember {}`) to avoid churn.
 *
 * @param items the values the spinner cycles through
 * @param changeListener the listener notified when the shown item changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param selectedIndex the index of the currently shown item (controlled)
 */
@Composable
public fun <T> Spinner(
    items: List<T>,
    changeListener: ChangeListener,
    modifier: SwingModifier = SwingModifier,
    selectedIndex: Int = 0,
) {
    SwingNode(
        factory = { JSpinner(SpinnerListModel(items)) },
        update = {
            set(items) {
                listModel(this).list = it
                applySelectedIndex(this, selectedIndex)
            }
            set(selectedIndex) { applySelectedIndex(this, it) }
            applyModifier(SwingModifier.changeListener(changeListener) then modifier)
        },
    )
}

/**
 * Re-applies [value] as the spinner's value, but only when it differs from the current value.
 *
 * A guard against re-setting an unchanged value keeps a programmatic set from echoing back through the
 * change listener as a spurious `onValueChange`.
 */
private fun applyValue(
    spinner: JSpinner,
    value: Any,
) {
    if (spinner.value == value) return
    spinner.value = value
}

/**
 * Reads the spinner's current value as [T] — the type the matching `Spinner` overload's model
 * produces. A clear failure here means the spinner was configured with a model whose value type the
 * overload's callback cannot represent (e.g. an integer overload over a non-integer model).
 */
private inline fun <reified T> JSpinner.valueAs(): T =
    value as? T
        ?: error(
            "Spinner value expected ${T::class.simpleName} but was ${value?.javaClass?.name}; " +
                "the Spinner overload requires a matching model type",
        )

/**
 * Re-applies [index] as the list spinner's selection by mapping it to the corresponding item, but only
 * when that item differs from the current value, so a programmatic set does not echo back through the
 * change listener.
 */
private fun applySelectedIndex(
    spinner: JSpinner,
    index: Int,
) {
    val list = listModel(spinner).list
    if (index !in list.indices) return
    val target = list[index]
    if (spinner.value == target) return
    spinner.value = target
}

/** Narrows the spinner's model to the [SpinnerNumberModel] the numeric wrappers install. */
private fun numberModel(spinner: JSpinner): SpinnerNumberModel = spinner.model as SpinnerNumberModel

/** Narrows the spinner's model to the [SpinnerListModel] the list wrapper installs. */
private fun listModel(spinner: JSpinner): SpinnerListModel = spinner.model as SpinnerListModel
