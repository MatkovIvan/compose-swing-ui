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
import javax.swing.JSlider
import javax.swing.event.ChangeListener

/**
 * A composable wrapper for JSlider.
 *
 * @param value the current value
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param onValueChange callback invoked when the value changes
 * @param min the minimum value
 * @param max the maximum value
 */
@Composable
public fun Slider(
    value: Int,
    modifier: SwingModifier = SwingModifier,
    onValueChange: (Int) -> Unit = {},
    min: Int = 0,
    max: Int = 100,
) {
    val callback = rememberUpdatedState(onValueChange)
    val listener = remember { ChangeListener { event -> callback.value((event.source as JSlider).value) } }
    Slider(value = value, changeListener = listener, modifier = modifier, min = min, max = max)
}

/**
 * A composable wrapper for JSlider driven by a raw [ChangeListener] instead of an `onValueChange`
 * lambda. The [changeListener] is attached as-is and removed on the same instance; pass a stable
 * instance (e.g. `remember {}`) to avoid a detach/re-attach on every recomposition.
 *
 * @param value the current value
 * @param changeListener the listener notified when the value changes
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param min the minimum value
 * @param max the maximum value
 */
@Composable
public fun Slider(
    value: Int,
    changeListener: ChangeListener,
    modifier: SwingModifier = SwingModifier,
    min: Int = 0,
    max: Int = 100,
) {
    SwingNode(
        factory = { JSlider(min, max, value) },
        update = {
            set(min) { this.minimum = it }
            set(max) { this.maximum = it }
            set(value) { this.value = it }
            applyModifier(SwingModifier.changeListener(changeListener) then modifier)
        },
    )
}
