package org.jetbrains.compose.swing.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.setContentAsInteropHost
import java.awt.Dialog
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JDialog
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/**
 * Composes a Dialog (JDialog) with the given content.
 *
 * The declarative control surface mirrors [Window] (`visible` + `onCloseRequest`, no imperative
 * handle), adding owner resolution and AWT [modality]. The dialog is owned by the nearest enclosing
 * [Window], read from [LocalWindow]; under a bare `application { }` scope with no window the dialog
 * is ownerless, and modal blocking only takes effect when there is an owning window.
 *
 * The dialog content runs as part of the enclosing (application or window) composition: state from
 * that scope and any [androidx.compose.runtime.CompositionLocal] provided above the dialog flow into
 * [content]. Descendants of the dialog read it as their [LocalWindow]. A shown dialog keeps
 * recomposing while it is visible.
 *
 * @param onCloseRequest callback to be called when the user attempts to close the dialog
 * @param title the title of the dialog
 * @param modality the AWT modality of the dialog; defaults to [Dialog.ModalityType.MODELESS] (the
 *   JDialog default)
 * @param size the initial size of the dialog
 * @param visible whether the dialog should be visible
 * @param resizable whether the dialog can be resized
 * @param content the composable content of the dialog
 */
@Composable
public fun Dialog(
    onCloseRequest: () -> Unit,
    title: String = "",
    modality: Dialog.ModalityType = Dialog.ModalityType.MODELESS,
    size: Dimension = Dimension(400, 300),
    visible: Boolean = true,
    resizable: Boolean = true,
    content: @Composable () -> Unit,
) {
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)
    val currentContent by rememberUpdatedState(content)
    val owner = LocalWindow.current

    // Capture the enclosing composition context (the application/window composition) here in the
    // composable body, NOT inside the DisposableEffect: the dialog's content pane is a detached
    // top-level peer, so the Swing-tree walk from it finds no parent. Threading this context through
    // explicitly makes the dialog content a CHILD of the enclosing composition, so app-scope state
    // and CompositionLocals flow into the dialog content. Same "preserve app->dialog flow" choice as
    // Window.
    val parentContext = rememberCompositionContext()

    DisposableEffect(Unit) {
        val dialog = JDialog(owner, modality)
        dialog.title = title
        dialog.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        dialog.size = size
        dialog.isResizable = resizable

        dialog.addWindowListener(
            object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    currentOnCloseRequest()
                }
            },
        )

        val handle =
            dialog.contentPane.setContentAsInteropHost(parentContext) {
                CompositionLocalProvider(LocalWindow provides dialog) {
                    currentContent()
                }
            }

        // Defer the show to a fresh EDT tick: a modal setVisible(true) blocks inside a nested
        // secondary event loop until the dialog hides, so it must run on a plain runnable rather than
        // inline in this effect body. The parent recomposer's continuations and frame-clock timer
        // firings are pumped by that same loop and keep recomposing the dialog content while it shows.
        SwingUtilities.invokeLater {
            dialog.isVisible = visible
        }

        onDispose {
            handle.dispose()
            dialog.dispose()
        }
    }
}
