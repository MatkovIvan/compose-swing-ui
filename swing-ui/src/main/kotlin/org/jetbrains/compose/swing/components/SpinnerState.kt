@file:JvmMultifileClass
@file:JvmName("ComponentsKt")

package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import javax.swing.SpinnerListModel
import javax.swing.SpinnerModel
import javax.swing.SpinnerNumberModel
import javax.swing.event.ChangeListener

/**
 * A hoistable state holder for a [Spinner] that owns the [SpinnerModel] the spinner renders. The state
 * and the bound spinner share one model, so a step taken through the spinner — an arrow press, typing, a
 * scroll — is reported by this state, and a value written through this state is what the spinner shows;
 * there is no value to keep in sync.
 *
 * [value] is snapshot-observable: reading it inside a composable subscribes to later model changes, so
 * the reader recomposes when the spinner's value changes for any reason.
 */
public class SpinnerState internal constructor(
    /** The [SpinnerModel] this state owns and the bound spinner renders. */
    public val model: SpinnerModel,
) : RememberObserver {
    // A snapshot-state mirror of the model's value. [changeListener] refreshes it on every model change,
    // so reading [value] registers a snapshot read that a later change — from the spinner or from a write
    // through [value] — invalidates for any reader.
    private var observedValue by mutableStateOf(model.value)

    private val changeListener = ChangeListener { observedValue = model.value }

    /**
     * The spinner's current value. Reading registers a snapshot subscription to later model changes;
     * assigning pushes the value into the model, which the bound spinner renders.
     */
    public var value: Any?
        get() = observedValue
        set(value) {
            // A no-op write would still echo back through the change listener as a spurious refresh;
            // only push a value that actually differs from what the model already holds.
            if (model.value != value) model.value = value
        }

    override fun onRemembered() {
        model.addChangeListener(changeListener)
        // Resync in case the model's value moved between construction and this state being remembered.
        observedValue = model.value
    }

    override fun onForgotten() {
        model.removeChangeListener(changeListener)
    }

    override fun onAbandoned(): Unit = onForgotten()
}

/**
 * Creates and remembers a [SpinnerState] over a [SpinnerNumberModel] that steps a numeric value between
 * [min] and [max] by [step]. A null bound is open on that side.
 *
 * [value] is the initial value only; it is owned by the returned state and driven afterwards through the
 * state's [SpinnerState.value]. The [min], [max] and [step] are declarative: a later change to any of
 * them updates the spinner in place, preserving the current value.
 *
 * @param value the value the spinner starts at.
 * @param min the smallest selectable value, or null for no lower bound.
 * @param max the largest selectable value, or null for no upper bound.
 * @param step the increment between adjacent values.
 */
@Composable
public fun rememberSpinnerState(
    value: Number,
    min: Number? = null,
    max: Number? = null,
    step: Number = 1,
): SpinnerState {
    // The general SpinnerNumberModel constructor takes Comparable minimum/maximum. Number is not itself
    // Comparable, but every concrete Number a caller passes (Int, Double, Long, ...) is Comparable at
    // runtime, so the star-projected cast of the bounds is sound.
    val model = remember { SpinnerNumberModel(value, min as Comparable<*>?, max as Comparable<*>?, step) }
    SideEffect {
        if (model.minimum != min) model.minimum = min as Comparable<*>?
        if (model.maximum != max) model.maximum = max as Comparable<*>?
        if (model.stepSize != step) model.stepSize = step
    }
    return remember { SpinnerState(model) }
}

/**
 * Creates and remembers a [SpinnerState] over a [SpinnerListModel] that cycles through [items], showing
 * the item at [selectedIndex]. An index outside the list leaves the model at its first item.
 *
 * [selectedIndex] is the initial selection only; the selected value is owned by the returned state and
 * driven afterwards through the state's [SpinnerState.value]. The [items] are declarative: a later change
 * updates the spinner in place, keeping the current selection where that item still exists.
 *
 * @param items the values the spinner cycles through.
 * @param selectedIndex the index of the item the spinner starts on.
 */
@Composable
public fun <T> rememberSpinnerState(
    items: List<T>,
    selectedIndex: Int = 0,
): SpinnerState {
    val model =
        remember {
            SpinnerListModel(items).apply {
                if (selectedIndex in items.indices) value = items[selectedIndex]
            }
        }
    SideEffect {
        // Setting the list keeps the current selection where the item still exists; it only jumps to the
        // first element when the current value has fallen out of the new list.
        if (model.list != items) model.list = items
    }
    return remember { SpinnerState(model) }
}
