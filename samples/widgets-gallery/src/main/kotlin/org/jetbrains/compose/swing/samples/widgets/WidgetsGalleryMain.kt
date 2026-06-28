package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.CompositionLocalProvider
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.window.LocalWindow
import java.awt.Dimension
import java.awt.Insets
import javax.swing.JFrame
import javax.swing.JMenuBar
import javax.swing.SwingUtilities
import javax.swing.UIManager

/*
 * The runnable entry point for the gallery — and ONLY the entry point.
 *
 * There is no `@Composable` in this file: window creation, the menu bar, the Look-and-Feel, and the
 * close behaviour are ordinary Swing/AWT plumbing, kept apart from the composable UI (the shell and the
 * sections, which know nothing about frames). That split is the lesson — a Compose-over-Swing app is a
 * thin Swing `main` that hands a `JFrame` to `setContent`, plus a reactive composable tree that is just
 * as happy running inside a headless test with no `main` at all.
 *
 * Two windows-into-Compose appear here:
 *  - The frame's content is composed with `frame.setContent { ... }`.
 *  - The menu bar is its OWN little composition via `menuBar.setContent { ... }`, so a composable menu
 *    can live in the native [JMenuBar].
 * The frame is also published through [LocalWindow], giving the modal dialog section a true owner.
 */
private const val WINDOW_WIDTH = 960
private const val WINDOW_HEIGHT = 680

fun main() {
    // All Swing work happens on the Event Dispatch Thread; both `setContent` calls are made from here.
    SwingUtilities.invokeLater {
        installLookAndFeel()

        val frame = JFrame("Compose Swing UI — Widgets gallery")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)
        frame.setLocationRelativeTo(null)

        val menuBar = JMenuBar()
        // Attach the menu bar to the frame BEFORE calling setContent as the preferred ordering: this lets
        // the menu content resolve its parent composition by walking up to the owning window's recomposer
        // immediately. Setting content on a detached menu bar is also supported — the bar simply defers
        // and mounts when it is later attached to the frame — but attaching first keeps the mount
        // synchronous.
        frame.jMenuBar = menuBar
        menuBar.setContent { ShowcaseMenuBar(onExit = { frame.dispose() }) }

        // Publish the frame so descendants (e.g. the modal dialog section) can resolve a real owner.
        frame.setContent {
            CompositionLocalProvider(LocalWindow provides frame) {
                ShowcaseShell()
            }
        }

        frame.isVisible = true
    }
}

/*
 * Visual polish is the host Look-and-Feel's job, not the framework's: install the platform's system LaF
 * so the gallery adopts the host's native chrome instead of the cross-platform Metal LaF, plus one
 * neutral tweak for breathing room. Theming beyond this belongs to whatever LaF the host application
 * chooses; the gallery deliberately ships none of its own.
 */
private fun installLookAndFeel() {
    runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
    UIManager.put("Button.margin", Insets(BUTTON_INSET_V, BUTTON_INSET_H, BUTTON_INSET_V, BUTTON_INSET_H))
}

private const val BUTTON_INSET_V = 4
private const val BUTTON_INSET_H = 12
