package org.jetbrains.compose.swing.samples.todo

import org.jetbrains.compose.swing.test.runSwingUiTest
import org.jetbrains.compose.swing.test.screenshot.assertImageMatches
import org.jetbrains.compose.swing.test.screenshot.captureToImage
import javax.swing.JProgressBar
import javax.swing.JTextField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Behavioral coverage for [ReactiveTaskList], driven entirely through the public test harness and the
 * sample's stable test tags. Each test exercises a user gesture where it really lands (a `doClick`, a
 * text replacement) and reads the live Swing tree the recomposition produced — never a private field or
 * composition slot.
 *
 * The starting fixture has four tasks, two of them already done, so "2 of 4 done" is the initial state.
 */
class ReactiveTaskListTest {
    @Test
    fun theSummaryAndProgressBarReflectTheInitialState() =
        runSwingUiTest {
            setContent { ReactiveTaskList() }

            onNodeWithText("2 of 4 done").assertExists()
            val progress = onNodeWithTag(PROGRESS_TAG).fetch<JProgressBar>()
            assertEquals(2, progress.value, "the progress bar mirrors the done count")
            assertEquals(4, progress.maximum, "the progress bar maximum mirrors the total count")
        }

    @Test
    fun completingATaskUpdatesTheDerivedSummaryAndProgress() =
        runSwingUiTest {
            setContent { ReactiveTaskList() }

            // Toggle the third task (id 3), which starts incomplete: the derived summary and the bar move
            // together, with no manual recomputation in the sample.
            onNodeWithTag(taskToggleTag(3)).performClick()

            onNodeWithText("3 of 4 done").assertExists()
            onNodeWithText("2 of 4 done").assertDoesNotExist()
            assertEquals(3, onNodeWithTag(PROGRESS_TAG).fetch<JProgressBar>().value)
        }

    @Test
    fun theAddButtonIsDisabledUntilTheDraftIsNonBlank() =
        runSwingUiTest {
            setContent { ReactiveTaskList() }

            onNodeWithTag(ADD_BUTTON_TAG).assertIsNotEnabled()

            onNodeWithTag(ADD_FIELD_TAG).performTextReplacement("Write docs")
            onNodeWithTag(ADD_BUTTON_TAG).assertIsEnabled()
        }

    @Test
    fun addingATaskInsertsAKeyedRowAndGrowsTheTotal() =
        runSwingUiTest {
            setContent { ReactiveTaskList() }

            // The fixture seeds ids 1..4, so a freshly added task takes id 5: a new keyed row appears and
            // the derived total grows from 4 to 5.
            onNodeWithTag(taskRowTag(5)).assertDoesNotExist()

            onNodeWithTag(ADD_FIELD_TAG).performTextReplacement("Ship the sample")
            onNodeWithTag(ADD_BUTTON_TAG).performClick()

            onNodeWithTag(taskRowTag(5)).assertExists()
            assertEquals(
                "Ship the sample",
                onNodeWithTag(taskTitleTag(5)).fetch<JTextField>().text,
                "the new row carries the typed title",
            )
            onNodeWithText("2 of 5 done").assertExists()
        }

    @Test
    fun removingATaskDropsItsRowFromTheTreeAndShrinksTheTotal() =
        runSwingUiTest {
            setContent { ReactiveTaskList() }

            onNodeWithTag(taskRowTag(2)).assertExists()

            // Removing the second task (a done one) drops its row entirely and recomputes the summary:
            // total 4 -> 3 and done 2 -> 1.
            onNodeWithTag(taskRemoveTag(2)).performClick()

            onNodeWithTag(taskRowTag(2)).assertDoesNotExist()
            onNodeWithText("1 of 3 done").assertExists()
        }

    @Test
    fun aTaskRowRendersToAStableBitmapScreenshotTest() =
        runSwingUiTest {
            setContent { ReactiveTaskList() }

            // Screenshot testing the sample: capture a tagged subtree as a real bitmap, then assert
            // against it with `assertImageMatches`, which compares two images by structural similarity
            // rather than peeking at pixels by hand.
            val row = onNodeWithTag(taskRowTag(3)).captureToImage()

            // The render is stable: capturing the same, unchanged row again matches the first capture.
            onNodeWithTag(taskRowTag(3)).assertImageMatches(expected = row)

            // A genuinely different element (a whole different-sized subtree) does NOT match it — the
            // comparison rejects it on the size mismatch alone, so the assertion throws.
            assertFailsWith<AssertionError> {
                onNodeWithTag(PROGRESS_TAG).assertImageMatches(expected = row)
            }
        }

    @Test
    fun renamingOneRowLeavesTheOtherRowsUntouched() =
        runSwingUiTest {
            setContent { ReactiveTaskList() }

            val otherTitleBefore = onNodeWithTag(taskTitleTag(1)).fetch<JTextField>().text

            onNodeWithTag(taskTitleTag(3)).performTextReplacement("Renamed task")

            assertEquals(
                "Renamed task",
                onNodeWithTag(taskTitleTag(3)).fetch<JTextField>().text,
                "the edited row holds the new title",
            )
            assertEquals(
                otherTitleBefore,
                onNodeWithTag(taskTitleTag(1)).fetch<JTextField>().text,
                "editing one keyed row must not disturb another row's content",
            )
        }
}
