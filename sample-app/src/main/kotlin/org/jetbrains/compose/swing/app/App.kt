package org.jetbrains.compose.swing.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.setContent
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

private const val WINDOW_WIDTH = 480
private const val WINDOW_HEIGHT = 240

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Compose Swing UI — Sample")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)
        frame.setContent { GreetingForm() }
        frame.isVisible = true
    }
}

@Composable
private fun GreetingForm() {
    var name by remember { mutableStateOf("") }
    var greeting by remember { mutableStateOf("Type your name and press Greet.") }

    SwingNode(factory = { JPanel(GridLayout(0, 1, 8, 8)) }) {
        Label("Name:")
        TextField(value = name, onValueChange = { name = it }, columns = 24)
        Button("Greet") { greeting = if (name.isBlank()) "Hello, stranger!" else "Hello, $name!" }
        Label(greeting)
    }
}
