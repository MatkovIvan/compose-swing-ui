package org.jetbrains.compose.swing.app

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.preferredSize
import org.jetbrains.compose.swing.setContent
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingUtilities

private const val WINDOW_WIDTH = 480
private const val WINDOW_HEIGHT = 240

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Compose Swing UI — Sample")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)
        frame.setContent { Greeting() }
        frame.isVisible = true
    }
}

@Composable
private fun Greeting() {
    SwingNode(
        factory = { JLabel("Hello from Compose-over-Swing") },
        update = {
            applyModifier(
                SwingModifier
                    .preferredSize(440, 80)
                    .testTag("greeting"),
            )
        },
    )
}
