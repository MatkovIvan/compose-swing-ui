package org.jetbrains.compose.swing.samples.todo

import org.jetbrains.compose.swing.setContent
import java.awt.Dimension
import java.awt.Insets
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager

/*
 * The runnable entry point — and ONLY the entry point.
 *
 * Notice there is not a single `@Composable` here: window creation, the Look-and-Feel, and the close
 * behaviour are ordinary Swing/AWT plumbing, kept apart from the composable UI (which lives in
 * `ReactiveTaskList.kt`). That separation is the lesson — a Compose-over-Swing app is a thin Swing
 * `main` that hands a `JFrame` to `setContent`, and a reactive composable tree that knows nothing about
 * frames or Look-and-Feels. You can drop the same composable into a test harness with no `main` at all.
 */
private const val WINDOW_WIDTH = 680
private const val WINDOW_HEIGHT = 620

fun main() {
    // All Swing work happens on the Event Dispatch Thread; `setContent` is called from here too.
    SwingUtilities.invokeLater {
        installLookAndFeel()

        val frame = JFrame("Compose Swing UI — To-do")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)
        frame.setLocationRelativeTo(null)

        // The one bridge from Swing to Compose: hand the frame a composable and let it manage the tree.
        frame.setContent {
            ReactiveTaskListScreen()
        }

        frame.isVisible = true
    }
}

/*
 * Visual polish is the host Look-and-Feel's job, not the framework's: install the platform's system LaF
 * so the sample adopts the host's native chrome instead of the cross-platform Metal LaF, plus one
 * neutral tweak for breathing room. Theming beyond this belongs to whatever LaF the host application
 * chooses; the sample deliberately ships none of its own.
 */
private fun installLookAndFeel() {
    runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
    UIManager.put("Button.margin", Insets(BUTTON_INSET_V, BUTTON_INSET_H, BUTTON_INSET_V, BUTTON_INSET_H))
}

private const val BUTTON_INSET_V = 4
private const val BUTTON_INSET_H = 12
