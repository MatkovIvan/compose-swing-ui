@file:JvmMultifileClass
@file:JvmName("SelectionComponentsKt")

package org.jetbrains.compose.swing.components.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.constants.BoxAxis
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.actionListener
import java.awt.event.ActionListener
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JPanel
import javax.swing.JRadioButton

/**
 * A composable wrapper for a group of mutually exclusive radio buttons backed by a shared
 * `ButtonGroup`, so at most one option is selected at a time.
 *
 * Declare the choices in [content]; each `option(...)` becomes a `JRadioButton` laid out in the group's
 * panel in call order. The selected option is controlled via [selectedIndex] (the zero-based position
 * of the chosen `option`, or any out-of-range value such as `-1` for no selection); clicking an option
 * selects it and invokes [onSelectionChange] with its index, while external [selectedIndex] changes
 * move the selection to match.
 *
 * ```
 * RadioGroup(selectedIndex = choice, onSelectionChange = { choice = it }) {
 *     option("Small")
 *     option("Medium")
 *     option("Large")
 * }
 * ```
 *
 * The options are laid out along [axis] in a panel; the axis is fixed when the group is first composed.
 *
 * @param selectedIndex the index of the selected option (controlled); an out-of-range value selects none
 * @param onSelectionChange callback invoked with the option's index when the user selects it
 * @param modifier the [SwingModifier] applied to the group's panel
 * @param axis the axis along which the options are arranged (a [BoxAxis] `BoxLayout` value)
 * @param content declares the options; see [RadioGroupScope]
 */
@Composable
public fun RadioGroup(
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: SwingModifier = SwingModifier,
    @BoxAxis axis: Int = BoxLayout.Y_AXIS,
    content: RadioGroupScope.() -> Unit,
) {
    // Collect the option declarations fresh on every composition so an option the caller stops
    // declaring (e.g. behind an `if`) drops out of `content` and its button is removed. A remembered,
    // mutated scope would retain the stale declaration.
    val scope = RadioGroupScopeImpl().apply(content)
    // The ButtonGroup is shared by every option for the lifetime of the group, so exclusion holds
    // across recompositions; each option joins on first composition and leaves on removal.
    val group = remember { ButtonGroup() }
    val onSelectionChangeState = rememberUpdatedState(onSelectionChange)

    SwingNode(
        factory = { JPanel().apply { layout = BoxLayout(this, axis) } },
        update = {
            applyModifier(modifier)
        },
        content = {
            scope.options.forEachIndexed { index, option ->
                // key() gives each option a stable composition identity by position; adding or removing
                // an option shifts later options' slots, and the applier installs/uninstalls buttons to
                // match while each leaves and rejoins the shared group through its onRelease.
                key(index) {
                    // A user click selects this button; report its index. A programmatic selectedIndex
                    // write does not fire this listener, so reflecting state never loops back here. The
                    // listener is stable for this option's slot and reads the latest callback.
                    val listener =
                        remember {
                            ActionListener { event ->
                                if ((event.source as JRadioButton).isSelected) onSelectionChangeState.value(index)
                            }
                        }
                    SwingNode(
                        factory = { JRadioButton() },
                        update = {
                            set(group) { it.add(this) }
                            set(option.text) { this.text = it }
                            // Selecting one member clears the rest through the shared group, so each
                            // option asserts only its own state from the controlled selectedIndex.
                            set(selectedIndex) { applySelected(this, index == it) }
                            applyModifier(SwingModifier.actionListener(listener) then option.modifier)
                        },
                        onRelease = { group.remove(this) },
                    )
                }
            }
        },
    )
}

/**
 * Declarative choices of a [RadioGroup]. Each [option] call appends one radio button, in call order;
 * its position is the index reported to [RadioGroup]'s `onSelectionChange` and matched against
 * `selectedIndex`.
 */
public interface RadioGroupScope {
    /**
     * Declares one choice.
     *
     * @param text the label shown next to the radio button
     * @param modifier the [SwingModifier] applied to this option's radio button
     */
    public fun option(
        text: String,
        modifier: SwingModifier = SwingModifier,
    )
}

/** One declared choice: its label and the [SwingModifier] for its button. */
private class RadioOption(
    val text: String,
    val modifier: SwingModifier,
)

private class RadioGroupScopeImpl : RadioGroupScope {
    val options: MutableList<RadioOption> = ArrayList()

    override fun option(
        text: String,
        modifier: SwingModifier,
    ) {
        options.add(RadioOption(text, modifier))
    }
}

/**
 * Re-applies [selected] to [button] only when it differs from the button's current state.
 *
 * Membership in a shared [ButtonGroup] makes selecting one button clear the others, so each option
 * asserts only its own state; guarding against re-selecting an unchanged button keeps a programmatic
 * controlled-selection write from doing redundant work. A programmatic `setSelected` does not fire the
 * button's action listener, so reflecting `selectedIndex` never echoes back as a spurious
 * `onSelectionChange`.
 */
private fun applySelected(
    button: JRadioButton,
    selected: Boolean,
) {
    if (button.isSelected == selected) return
    button.isSelected = selected
}
