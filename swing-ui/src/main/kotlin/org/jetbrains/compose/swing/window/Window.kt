package org.jetbrains.compose.swing.window

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.swing.GlobalSnapshotManager
import org.jetbrains.compose.swing.SwingApplier
import org.jetbrains.compose.swing.SwingDispatcher
import org.jetbrains.compose.swing.setContent
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/**
 * Composes a Window (JFrame) with the given content.
 *
 * @param onCloseRequest callback to be called when the user attempts to close the window
 * @param title the title of the window
 * @param size the initial size of the window
 * @param visible whether the window should be visible
 * @param resizable whether the window can be resized
 * @param content the composable content of the window
 */
@Composable
fun Window(
    onCloseRequest: () -> Unit,
    title: String = "Compose Swing Window",
    size: Dimension = Dimension(800, 600),
    visible: Boolean = true,
    resizable: Boolean = true,
    content: @Composable () -> Unit
) {
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)
    val currentContent by rememberUpdatedState(content)
    
    DisposableEffect(Unit) {
        val frame = JFrame(title)
        frame.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        frame.size = size
        frame.isResizable = resizable
        
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                currentOnCloseRequest()
            }
        })
        
        frame.setContent {
            currentContent()
        }
        
        frame.isVisible = visible
        
        onDispose {
            frame.dispose()
        }
    }
}

/**
 * Sets the composable content of a JFrame.
 * This is similar to setContent in Compose Multiplatform.
 * 
 * Uses the global Recomposer and supports parent composition lookup via client properties.
 *
 * @param content the composable content to set
 */
fun JFrame.setContent(content: @Composable () -> Unit) {
    contentPane.setContent(content)
}
