@file:JvmMultifileClass
@file:JvmName("ComponentsKt")

package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import javax.swing.JProgressBar

/**
 * A composable wrapper for JProgressBar.
 *
 * @param modifier the [SwingModifier] applied to the underlying component
 * @param value the current value
 * @param min the minimum value
 * @param max the maximum value
 * @param indeterminate whether the progress bar is indeterminate
 */
@Composable
public fun ProgressBar(
    modifier: SwingModifier = SwingModifier,
    value: Int = 0,
    min: Int = 0,
    max: Int = 100,
    indeterminate: Boolean = false,
) {
    SwingNode(
        factory = { JProgressBar(min, max) },
        update = {
            set(min) { this.minimum = it }
            set(max) { this.maximum = it }
            set(value) { this.value = it }
            set(indeterminate) { this.isIndeterminate = it }
            applyModifier(modifier)
        },
    )
}
