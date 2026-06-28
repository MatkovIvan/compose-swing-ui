package org.jetbrains.compose.swing.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.setContentAsInteropHost
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.WindowConstants

/**
 * Composes a Window (JFrame) with the given content.
 *
 * The window content runs as part of the enclosing application composition: state held in the
 * application scope and any [androidx.compose.runtime.CompositionLocal] provided above the window
 * flow into [content], and the content keeps recomposing while the window is shown.
 *
 * The [title], [size], [visible] and [resizable] arguments are reactive: changing any of them in a
 * recomposition updates the realized window accordingly.
 *
 * @param onCloseRequest callback to be called when the user attempts to close the window
 * @param title the title of the window
 * @param size the size of the window
 * @param visible whether the window should be visible
 * @param resizable whether the window can be resized
 * @param content the composable content of the window
 */
@Composable
public fun Window(
    onCloseRequest: () -> Unit,
    title: String = "Compose Swing Window",
    size: Dimension = Dimension(800, 600),
    visible: Boolean = true,
    resizable: Boolean = true,
    content: @Composable () -> Unit,
) {
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)
    val currentContent by rememberUpdatedState(content)

    // Capture the enclosing composition context (the application composition) here in the composable
    // body, NOT inside the DisposableEffect: the window's content pane is a detached top-level peer,
    // so the Swing-tree walk from it finds no parent. Threading this context through explicitly makes
    // the frame content a CHILD of the application composition, so app-scope state and
    // CompositionLocals flow into the window content. This is the deliberate "preserve app->window
    // flow" choice: a window created declaratively under application { } stays a child of the app
    // composition rather than spinning up its own window-local recomposer.
    val parentContext = rememberCompositionContext()

    val frame =
        remember {
            JFrame().also { it.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE }
        }

    DisposableEffect(Unit) {
        val listener =
            object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    currentOnCloseRequest()
                }
            }
        frame.addWindowListener(listener)

        val handle =
            frame.contentPane.setContentAsInteropHost(parentContext) {
                CompositionLocalProvider(LocalWindow provides frame) {
                    currentContent()
                }
            }

        onDispose {
            handle.dispose()
            frame.removeWindowListener(listener)
            frame.dispose()
        }
    }

    // Tracks the last size requested through the [size] argument so that the window is resized only
    // when that argument changes, never clobbering a user-driven resize.
    val appliedSize = remember { arrayOfNulls<Dimension>(1) }

    // Reactive params: re-applied whenever the corresponding argument changes across recomposition.
    // Effect bodies run on the composition's Swing dispatcher (the EDT), so these mutations are
    // EDT-safe.
    SideEffect {
        if (frame.title != title) frame.title = title
        if (frame.isResizable != resizable) frame.isResizable = resizable
        if (appliedSize[0] != size) {
            appliedSize[0] = size
            frame.size = size
        }
        if (frame.isVisible != visible) frame.isVisible = visible
    }
}
