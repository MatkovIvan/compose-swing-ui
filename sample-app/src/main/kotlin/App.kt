package org.jetbrains.compose.swing.app

import androidx.compose.runtime.*
import org.jetbrains.compose.swing.components.*
import org.jetbrains.compose.swing.window.setContent
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * Sample application demonstrating Compose Swing UI wrappers.
 * 
 * This sample shows how to use:
 * - JFrame.setContent for composable content
 * - Basic components (Button, Label, TextField, etc.)
 * - Layout panels (FlowPanel, BoxPanel, BorderPanel, GridPanel)
 * - JMenuBar.setContent for composable menus
 */
fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Compose Swing UI Sample")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(800, 600)
        
        // Create menu bar with composable content
        val menuBar = JMenuBar()
        menuBar.setContent {
            Menu("File") {
                MenuItem("New", onClick = { println("New clicked") })
                MenuItem("Open", onClick = { println("Open clicked") })
                MenuSeparator()
                MenuItem("Exit", onClick = { System.exit(0) })
            }
            Menu("Edit") {
                MenuItem("Cut", onClick = { println("Cut clicked") })
                MenuItem("Copy", onClick = { println("Copy clicked") })
                MenuItem("Paste", onClick = { println("Paste clicked") })
            }
            Menu("View") {
                CheckBoxMenuItem("Show Toolbar", checked = true, onCheckedChange = { 
                    println("Show Toolbar: $it") 
                })
                CheckBoxMenuItem("Show Status Bar", checked = false, onCheckedChange = { 
                    println("Show Status Bar: $it") 
                })
            }
        }
        frame.jMenuBar = menuBar
        
        // Set composable content for the frame
        frame.setContent {
            SampleApp()
        }
        
        frame.isVisible = true
    }
}

@Composable
fun SampleApp() {
    var textFieldValue by remember { mutableStateOf("Hello, Compose Swing!") }
    var textAreaValue by remember { mutableStateOf("This is a text area.\nYou can type multiple lines here.") }
    var checkBoxChecked by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(50) }
    var comboBoxSelection by remember { mutableStateOf(0) }
    var counter by remember { mutableStateOf(0) }
    
    BorderPanel {
        // Top panel with title
        Panel(layout = BorderLayout()) {
            Label(
                text = "Compose Swing UI - Component Showcase",
                horizontalAlignment = SwingConstants.CENTER
            )
        }
        
        // Center panel with main content
        BoxPanel(axis = BoxLayout.Y_AXIS) {
            // Counter section
            FlowPanel {
                Label("Counter: $counter")
                Button("Increment", onClick = { counter++ })
                Button("Decrement", onClick = { counter-- })
                Button("Reset", onClick = { counter = 0 })
            }
            
            Separator()
            
            // Text input section
            FlowPanel {
                Label("TextField:")
                TextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    columns = 30
                )
            }
            
            // Text area section
            FlowPanel {
                Label("TextArea:")
            }
            FlowPanel {
                TextArea(
                    value = textAreaValue,
                    onValueChange = { textAreaValue = it },
                    rows = 4,
                    columns = 40
                )
            }
            
            Separator()
            
            // Checkbox and slider section
            FlowPanel {
                CheckBox(
                    text = "Enable features",
                    checked = checkBoxChecked,
                    onCheckedChange = { checkBoxChecked = it }
                )
                Label("Slider value: $sliderValue")
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    min = 0,
                    max = 100
                )
            }
            
            Separator()
            
            // ComboBox section
            FlowPanel {
                Label("Select option:")
                ComboBox(
                    items = listOf("Option 1", "Option 2", "Option 3", "Option 4"),
                    selectedIndex = comboBoxSelection,
                    onSelectionChange = { comboBoxSelection = it }
                )
                Label("Selected: ${if (comboBoxSelection >= 0) "Option ${comboBoxSelection + 1}" else "None"}")
            }
            
            Separator()
            
            // Progress bar section
            FlowPanel {
                Label("Progress:")
                ProgressBar(value = sliderValue, min = 0, max = 100)
            }
            
            Separator()
            
            // Radio buttons section
            FlowPanel {
                Label("Choose one:")
                var selectedRadio by remember { mutableStateOf(0) }
                RadioButton(
                    text = "Option A",
                    selected = selectedRadio == 0,
                    onSelect = { selectedRadio = 0 }
                )
                RadioButton(
                    text = "Option B",
                    selected = selectedRadio == 1,
                    onSelect = { selectedRadio = 1 }
                )
                RadioButton(
                    text = "Option C",
                    selected = selectedRadio == 2,
                    onSelect = { selectedRadio = 2 }
                )
            }
            
            Separator()
            
            // Grid layout example
            GridPanel(rows = 2, cols = 3, hgap = 5, vgap = 5) {
                Button("Grid 1", onClick = { println("Grid 1 clicked") })
                Button("Grid 2", onClick = { println("Grid 2 clicked") })
                Button("Grid 3", onClick = { println("Grid 3 clicked") })
                Button("Grid 4", onClick = { println("Grid 4 clicked") })
                Button("Grid 5", onClick = { println("Grid 5 clicked") })
                Button("Grid 6", onClick = { println("Grid 6 clicked") })
            }
        }
        
        // Bottom panel with status
        FlowPanel {
            Label("Status: Ready | Counter: $counter | CheckBox: ${if (checkBoxChecked) "Checked" else "Unchecked"}")
        }
    }
}
