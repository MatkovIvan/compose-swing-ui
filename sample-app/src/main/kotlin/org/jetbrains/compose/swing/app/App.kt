package org.jetbrains.compose.swing.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.jetbrains.compose.swing.animation.core.ExperimentalSwingAnimationApi
import org.jetbrains.compose.swing.animation.core.animateIntAsState
import org.jetbrains.compose.swing.components.Canvas
import org.jetbrains.compose.swing.components.CheckBoxMenuItem
import org.jetbrains.compose.swing.components.ComboBox
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.Menu
import org.jetbrains.compose.swing.components.MenuItem
import org.jetbrains.compose.swing.components.MenuSeparator
import org.jetbrains.compose.swing.components.ProgressBar
import org.jetbrains.compose.swing.components.Separator
import org.jetbrains.compose.swing.components.Slider
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.button.RadioButton
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.components.layout.BoxPanel
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.components.layout.GridPanel
import org.jetbrains.compose.swing.components.selection.ListBox
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.dialogs.showMessageDialog
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.preferredSize
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.window.LocalWindow
import java.awt.Color
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JMenuBar
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

private const val WINDOW_WIDTH = 820
private const val WINDOW_HEIGHT = 640

private val FRUITS = listOf("Apple", "Banana", "Cherry", "Date")

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Compose Swing UI — Component Showcase")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)

        val menuBar = JMenuBar()
        frame.jMenuBar = menuBar
        menuBar.setContent { ShowcaseMenuBar(onExit = { frame.dispose() }) }

        frame.setContent {
            CompositionLocalProvider(LocalWindow provides frame) {
                ShowcaseShell()
            }
        }
        frame.isVisible = true
    }
}

@Composable
private fun ShowcaseMenuBar(onExit: () -> Unit) {
    Menu("File") {
        MenuItem("New") { println("New") }
        MenuItem("Open") { println("Open") }
        MenuSeparator()
        MenuItem("Exit") { onExit() }
    }
    Menu("View") {
        var toolbar by remember { mutableStateOf(true) }
        CheckBoxMenuItem("Show Toolbar", checked = toolbar, onCheckedChange = { toolbar = it })
    }
}

@Composable
private fun ShowcaseShell() {
    var enabled by remember { mutableStateOf(false) }
    var slider by remember { mutableStateOf(50) }

    BorderPanel {
        north {
            Label("Compose Swing UI — Component Showcase", horizontalAlignment = SwingConstants.CENTER)
        }
        center {
            BoxPanel {
                CounterRow()
                Separator()
                TextRow()
                ToggleRow(
                    enabled = enabled,
                    onEnabledChange = { enabled = it },
                    slider = slider,
                    onSliderChange = { slider = it },
                )
                ChoiceRow(progress = slider)
                RadioRow()
                SelectionRow(fillPercent = slider)
            }
        }
        south {
            Label("Status: ready | features=${if (enabled) "on" else "off"}")
        }
    }
}

@Composable
private fun CounterRow() {
    val scope = rememberCoroutineScope()
    val window = LocalWindow.current
    var counter by remember { mutableStateOf(0) }
    FlowPanel {
        Label("Counter: $counter")
        Button("Increment") { counter++ }
        Button("Reset") { counter = 0 }
        Button("About") {
            scope.launch {
                showMessageDialog("Compose-over-Swing showcase.", parent = window, title = "About")
            }
        }
    }
}

@Composable
private fun TextRow() {
    var text by remember { mutableStateOf("Hello, Compose Swing!") }
    FlowPanel {
        Label("Text:")
        TextField(value = text, onValueChange = { text = it }, columns = 24)
    }
}

@Composable
private fun ToggleRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    slider: Int,
    onSliderChange: (Int) -> Unit,
) {
    FlowPanel {
        CheckBox("Enable features", checked = enabled, onCheckedChange = onEnabledChange)
        Label("Slider: $slider")
        Slider(value = slider, onValueChange = onSliderChange)
    }
}

@Composable
private fun ChoiceRow(progress: Int) {
    var choice by remember { mutableStateOf(0) }
    FlowPanel {
        Label("Option:")
        ComboBox(
            items = listOf("One", "Two", "Three"),
            selectedIndex = choice,
            onSelectionChange = { choice = it },
        )
        ProgressBar(value = progress)
    }
}

@Composable
private fun RadioRow() {
    var radio by remember { mutableStateOf(0) }
    FlowPanel {
        Label("Pick one:")
        RadioButton("A", selected = radio == 0, onSelect = { radio = 0 })
        RadioButton("B", selected = radio == 1, onSelect = { radio = 1 })
        RadioButton("C", selected = radio == 2, onSelect = { radio = 2 })
    }
}

@OptIn(ExperimentalSwingAnimationApi::class)
@Composable
private fun SelectionRow(fillPercent: Int) {
    var fruit by remember { mutableStateOf(listOf(0)) }
    val fill by animateIntAsState(targetValue = fillPercent, label = "canvasFill")
    GridPanel(rows = 1, cols = 2, hgap = 8, vgap = 8) {
        ListBox(
            items = FRUITS,
            selectedIndices = fruit,
            onSelectionChange = { fruit = it },
            selectionMode = ListSelectionModel.SINGLE_SELECTION,
            visibleRowCount = 4,
        )
        Canvas(modifier = SwingModifier.preferredSize(160, 120)) { g, width, height ->
            g.color = Color(0x33, 0x66, 0xCC)
            g.fillRect(0, 0, width * fill / 100, height)
        }
    }
}
