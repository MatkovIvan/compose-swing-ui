package org.jetbrains.compose.swing.samples.todo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.ProgressBar
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.components.layout.BoxPanel
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.components.layout.ScrollPane
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.interaction.enabled
import javax.swing.BoxLayout
import javax.swing.SwingConstants

/**
 * A reactive to-do list that shows what Compose-over-Swing buys you on a single, approachable screen.
 *
 * One [SnapshotStateList] of [Task]s is the single source of truth. Adding, removing, and toggling a
 * task mutates that list; every piece of UI that reads it — the live "N of M done" summary, the
 * progress bar, and the rows themselves — recomposes automatically. The summary is a [derivedStateOf]
 * so it recomputes only when the list actually changes, never on unrelated recompositions. Each row is
 * keyed by a stable task id, so adding or completing one task never disturbs the identity (or the
 * in-progress text edit) of any other.
 */
@Composable
internal fun ReactiveTaskList() {
    val tasks = remember { initialTasks().toMutableStateList() }
    var nextId by remember { mutableIntStateOf(tasks.size + 1) }

    // Derived, not recomputed by hand: these read `tasks` and refresh only when it changes.
    val total by remember { derivedStateOf { tasks.size } }
    val done by remember { derivedStateOf { tasks.count { it.done } } }

    BorderPanel(modifier = SwingModifier.testTag(TASK_LIST_TAG), hgap = 0, vgap = ROW_GAP) {
        north {
            BoxPanel(axis = BoxLayout.Y_AXIS) {
                SampleTitle("Reactive task list")
                Caption(
                    "One state list drives the summary, the progress bar, and the rows. " +
                        "Edit, add, complete, or remove a task and watch the whole screen follow.",
                )
                SummaryRow(done = done, total = total)
                AddTaskRow(onAdd = { title -> tasks += Task(id = nextId++, title = title) })
            }
        }
        center {
            TaskRows(
                tasks = tasks,
                onToggle = { id, checked -> tasks.replaceById(id) { it.copy(done = checked) } },
                onRename = { id, title -> tasks.replaceById(id) { it.copy(title = title) } },
                onRemove = { id -> tasks.removeAll { it.id == id } },
            )
        }
    }
}

/** The live progress summary: a "N of M done" label plus a progress bar, both derived from the list. */
@Composable
private fun SummaryRow(
    done: Int,
    total: Int,
) {
    Card("Progress") {
        FlowPanel(alignment = SwingConstants.LEADING) {
            // Fixed-width so the bar to its right never shifts as the counts change.
            ValueLabel(text = "$done of $total done", width = 120)
            ProgressBar(
                value = done,
                min = 0,
                max = total.coerceAtLeast(1),
                modifier = SwingModifier.testTag(PROGRESS_TAG),
            )
        }
    }
}

/** The add-a-task row: a text field plus an Add button that is disabled while the field is blank. */
@Composable
private fun AddTaskRow(onAdd: (String) -> Unit) {
    Card("Add a task") {
        var draft by remember { mutableStateOf("") }
        val canAdd by remember { derivedStateOf { draft.isNotBlank() } }

        fun commit() {
            if (draft.isNotBlank()) {
                onAdd(draft.trim())
                draft = ""
            }
        }

        FlowPanel(alignment = SwingConstants.LEADING) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                columns = 28,
                modifier = SwingModifier.testTag(ADD_FIELD_TAG),
            )
            Button(
                text = "Add",
                onClick = { commit() },
                modifier = SwingModifier.enabled(canAdd).testTag(ADD_BUTTON_TAG),
            )
        }
    }
}

/**
 * The dynamic list hierarchy. Each task becomes one keyed row; the set of rows grows and shrinks with
 * the list, demonstrating structural recomposition (rows are inserted and removed from the live Swing
 * tree, not merely shown and hidden). An empty list collapses to a single placeholder row.
 */
@Composable
private fun TaskRows(
    tasks: List<Task>,
    onToggle: (Int, Boolean) -> Unit,
    onRename: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
) {
    ScrollPane {
        content {
            BoxPanel(axis = BoxLayout.Y_AXIS) {
                if (tasks.isEmpty()) {
                    Card("Tasks") {
                        Label("No tasks yet — add one above.")
                    }
                } else {
                    tasks.forEach { task ->
                        // Keyed by the stable task id so a row keeps its identity (and any in-progress
                        // edit) when other rows are added, removed, or reordered around it.
                        key(task.id) {
                            TaskRow(
                                task = task,
                                onToggle = { checked -> onToggle(task.id, checked) },
                                onRename = { title -> onRename(task.id, title) },
                                onRemove = { onRemove(task.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single task row: a completion checkbox, an inline-editable title, and a remove button — laid out so
 * toggling completion never changes the row's size or the position of any control. The checkbox sits in
 * a fixed-width west slot, so its label width changing ("Done" vs unchecked) cannot push the title.
 */
@Composable
private fun TaskRow(
    task: Task,
    onToggle: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onRemove: () -> Unit,
) {
    BorderPanel(modifier = SwingModifier.testTag(taskRowTag(task.id)), hgap = ROW_GAP, vgap = 0) {
        west {
            // Checkbox text is empty and fixed in place, so checking it never reflows the title field.
            CheckBox(
                text = "",
                checked = task.done,
                onCheckedChange = onToggle,
                modifier = SwingModifier.testTag(taskToggleTag(task.id)),
            )
        }
        center {
            TextField(
                value = task.title,
                onValueChange = onRename,
                columns = 28,
                modifier = SwingModifier.testTag(taskTitleTag(task.id)),
            )
        }
        east {
            Button(
                text = "Remove",
                onClick = onRemove,
                modifier = SwingModifier.testTag(taskRemoveTag(task.id)),
            )
        }
    }
}

/** Replaces the single task with [id] by applying [transform] to it; a no-op if no such task exists. */
private fun SnapshotStateList<Task>.replaceById(
    id: Int,
    transform: (Task) -> Task,
) {
    val index = indexOfFirst { it.id == id }
    if (index >= 0) this[index] = transform(this[index])
}

/** An immutable task. State changes produce a fresh copy, so the list's diffing stays value-based. */
internal data class Task(
    val id: Int,
    val title: String,
    val done: Boolean = false,
)

private fun initialTasks(): List<Task> =
    listOf(
        Task(id = 1, title = "Set the system Look-and-Feel", done = true),
        Task(id = 2, title = "Compose a reactive screen over Swing", done = true),
        Task(id = 3, title = "Drive the list from a single state source"),
        Task(id = 4, title = "Keep the layout still as state changes"),
    )

// Test tags — the consumer test drives and reads the sample through these stable handles, not through
// brittle text or tree-position queries.
internal const val TASK_LIST_TAG: String = "reactive-task-list"
internal const val PROGRESS_TAG: String = "task-progress"
internal const val ADD_FIELD_TAG: String = "task-add-field"
internal const val ADD_BUTTON_TAG: String = "task-add-button"

internal fun taskRowTag(id: Int): String = "task-row-$id"

internal fun taskToggleTag(id: Int): String = "task-toggle-$id"

internal fun taskTitleTag(id: Int): String = "task-title-$id"

internal fun taskRemoveTag(id: Int): String = "task-remove-$id"
