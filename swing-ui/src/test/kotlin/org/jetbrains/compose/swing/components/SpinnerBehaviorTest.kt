package org.jetbrains.compose.swing.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.onNodeOfType
import org.jetbrains.compose.swing.test.runSwingUiTest
import javax.swing.JSpinner
import javax.swing.SpinnerListModel
import javax.swing.SpinnerNumberModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavioral tests for [Spinner], driven through the real composition pipeline and asserting against
 * the live `JSpinner` and its model.
 *
 * The central guarantees: the value and bounds render into the number model; a change originating from
 * the spinner fires `onValueChange`; a controlled value update applies without echoing back as a
 * spurious callback; and the list variant cycles through its items by index.
 */
class SpinnerBehaviorTest {
    private fun SwingUiTest.spinner(): JSpinner = onNodeOfType<JSpinner>().fetch()

    @Test
    fun valueAndBoundsRenderIntoTheNumberModel() = runSwingUiTest {
        setContent { Spinner(value = 5, min = 0, max = 10, step = 2) }

        val model = spinner().model as SpinnerNumberModel
        assertEquals(5, model.value, "the model should render the initial value")
        assertEquals(0, model.minimum, "the model should render the minimum bound")
        assertEquals(10, model.maximum, "the model should render the maximum bound")
        assertEquals(2, model.stepSize, "the model should render the step size")
    }

    @Test
    fun steppingTheSpinnerFiresOnValueChange() = runSwingUiTest {
        val received = mutableListOf<Int>()
        setContent { Spinner(value = 5, min = 0, max = 10, onValueChange = { received += it }) }

        val spinner = spinner()
        // getNextValue is what the up arrow commits; setting it drives the same model write path.
        spinner.value = spinner.model.nextValue
        awaitIdle()

        assertEquals(listOf(6), received)
    }

    @Test
    fun controlledValueUpdateConvergesWithoutALoop() = runSwingUiTest {
        var value by mutableIntStateOf(5)
        val received = mutableListOf<Int>()
        setContent {
            Spinner(
                value = value,
                min = 0,
                max = 10,
                onValueChange = {
                    received += it
                    value = it
                },
            )
        }

        val spinner = spinner()
        assertEquals(5, spinner.value, "the spinner should start at the controlled value")

        // A purely external value update applies to the spinner and settles. The guard skips
        // re-applying an unchanged value, so any echoed callback carries the SAME value the
        // controller already holds and does not bounce back.
        value = 8
        awaitIdle()

        assertEquals(8, spinner.value, "external value applied")
        assertEquals(8, value, "controlled state settled on the new value")
        assertTrue(received.all { it == 8 }, "value oscillated instead of converging: $received")

        val callbacksAfterSettle = received.size
        awaitIdle()
        assertEquals(callbacksAfterSettle, received.size, "spinner kept firing callbacks after settling")
    }

    @Test
    fun doubleSpinnerStepsByFractionalStep() = runSwingUiTest {
        val received = mutableListOf<Double>()
        setContent {
            Spinner(value = 1.0, min = 0.0, max = 2.0, step = 0.25, onValueChange = { received += it })
        }

        val spinner = spinner()
        spinner.value = spinner.model.nextValue
        awaitIdle()

        assertEquals(listOf(1.25), received)
    }

    @Test
    fun listSpinnerCyclesThroughItemsByIndex() = runSwingUiTest {
        val received = mutableListOf<Int>()
        setContent {
            Spinner(
                items = listOf("red", "green", "blue"),
                selectedIndex = 0,
                onSelectionChange = { received += it },
            )
        }

        val spinner = spinner()
        assertEquals("red", spinner.value, "the list spinner should start at the first item")

        // The up arrow advances to the next list element; driving nextValue takes the same path.
        spinner.value = spinner.model.nextValue
        awaitIdle()

        assertEquals("green", spinner.value, "advancing should move to the next list item")
        assertEquals(listOf(1), received, "advancing the list spinner reports the new index")
    }

    @Test
    fun listSpinnerReAppliesSelectionAfterItemsChange() = runSwingUiTest {
        var items by mutableStateOf(listOf("a", "b", "c"))
        setContent { Spinner(items = items, selectedIndex = 1) }

        val spinner = spinner()
        assertEquals("b", spinner.value, "the spinner should start at the controlled index")
        assertEquals(
            listOf("a", "b", "c"),
            (spinner.model as SpinnerListModel).list,
            "the model should hold the initial items",
        )

        // Replacing the list resets the model value to its first element; the wrapper must re-apply
        // selectedIndex so the controlled selection survives the items change.
        items = listOf("x", "y", "z")
        awaitIdle()

        assertEquals(
            listOf("x", "y", "z"),
            (spinner.model as SpinnerListModel).list,
            "the model should hold the replaced items",
        )
        assertEquals("y", spinner.value, "controlled index re-applied after items change")
    }
}
