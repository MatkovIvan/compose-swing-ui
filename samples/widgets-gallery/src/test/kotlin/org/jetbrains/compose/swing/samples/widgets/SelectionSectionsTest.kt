package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.onAllNodesOfType
import org.jetbrains.compose.swing.test.onNodeOfType
import org.jetbrains.compose.swing.test.runSwingUiTest
import javax.swing.JRadioButton
import javax.swing.JTable
import javax.swing.JTree
import kotlin.test.Test

/**
 * Behavioral coverage for the selection-driven sections — RadioGroup, Table, and Tree. Each section
 * holds its selection in a single Compose state and echoes it into a Label; the tests drive the real
 * selection gesture (clicking a radio, selecting a table row, selecting a tree row) and read the echo
 * the wrapper's listener and recomposition produce.
 */
class SelectionSectionsTest {
    @Test
    fun theRadioGroupSelectsExactlyOneOption() =
        runSwingUiTest {
            openSection("RadioGroup")

            // The vertical group starts on the first plan and the horizontal group on its middle size.
            onNodeWithText("Selected plan: Free", substring = true).assertExists()
            onNodeWithText("Selected size: Medium", substring = true).assertExists()

            // Selecting another radio moves the single selection there; the echo names only that choice.
            onNodeWithText("Team").performClick()
            onNodeWithText("Selected plan: Team", substring = true).assertExists()
            // The previous selection is gone — a RadioGroup is mutually exclusive.
            onNodeWithText("Selected plan: Free", substring = true).assertDoesNotExist()
        }

    @Test
    fun theRadioGroupRendersOneButtonPerOption() =
        runSwingUiTest {
            openSection("RadioGroup")

            // The vertical group declares four plans and the horizontal group three sizes: seven radios.
            onAllNodesOfType<JRadioButton>().assertCountEquals(VERTICAL_PLANS + HORIZONTAL_SIZES)
        }

    @Test
    fun theTableSelectionFeedsTheEcho() =
        runSwingUiTest {
            openSection("Table")

            // The table starts with the first row selected; selecting another row updates the echo.
            onNodeWithText("Selected: Ada Lovelace", substring = true).assertExists()

            val table = onNodeOfType<JTable>().fetch<JTable>()
            table.setRowSelectionInterval(2, 2)
            awaitIdle()
            onNodeWithText("Selected: Grace Hopper", substring = true).assertExists()
        }

    @Test
    fun theTableProjectsTypedRowsThroughItsColumns() =
        runSwingUiTest {
            openSection("Table")

            val table = onNodeOfType<JTable>().fetch<JTable>()
            // Three declared columns (Name, Role, Age) over four people rows.
            kotlin.test.assertEquals(TABLE_COLUMNS, table.columnCount)
            kotlin.test.assertEquals(TABLE_ROWS, table.rowCount)
            // The projection runs each row through its column lambdas; the first cell is the first name.
            kotlin.test.assertEquals("Ada Lovelace", table.getValueAt(0, 0))
        }

    @Test
    fun theTreeSelectionResolvesBackToReadableNames() =
        runSwingUiTest {
            openSection("Tree")

            // Nothing is selected on entry.
            onNodeWithText("Selected path: (none)", substring = true).assertExists()

            // Selecting the root row drives onSelectionChange, and the echo resolves the path to names.
            val tree = onNodeOfType<JTree>().fetch<JTree>()
            tree.setSelectionRow(0)
            awaitIdle()
            onNodeWithText("Selected path: Project", substring = true).assertExists()
        }

    private companion object {
        const val VERTICAL_PLANS = 4
        const val HORIZONTAL_SIZES = 3
        const val TABLE_COLUMNS = 3
        const val TABLE_ROWS = 4
    }
}
