package org.jetbrains.compose.swing.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Dialog
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
 * The [title], [visible] and [resizable] arguments are reactive: changing any of them in a
 * recomposition updates the realized dialog accordingly. Geometry is driven by [state], which is
 * two-way: assigning to [DialogState.position]/[DialogState.size] repositions or resizes the dialog,
 * and a user dragging or resizing the dialog writes the new geometry back into [state].
 *
 * @param onCloseRequest callback to be called when the user attempts to close the dialog
 * @param state the hoistable, observable geometry (position and size) of the dialog
 * @param title the title of the dialog
 * @param modality the AWT modality of the dialog; defaults to [Dialog.ModalityType.MODELESS] (the
 *   JDialog default)
 * @param visible whether the dialog should be visible
 * @param resizable whether the dialog can be resized
 * @param content the composable content of the dialog
 */
@Composable
public fun Dialog(
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(),
    title: String = "",
    modality: Dialog.ModalityType = Dialog.ModalityType.MODELESS,
    visible: Boolean = true,
    resizable: Boolean = true,
    content: @Composable () -> Unit,
) {
    val owner = LocalWindow.current

    // Owner and modality are fixed for the dialog's lifetime, so the peer is created once.
    val dialog =
        remember {
            JDialog(owner, modality).also { it.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE }
        }

    // Holds the geometry that is currently in sync between [state] and the realized dialog. Shared by
    // the apply and the write-back listener so the two directions never fight.
    val appliedGeometry = remember { AppliedGeometry() }

    // Tracks the latest visibility requested on the dialog, or null while there is no request to
    // honor. A modal show is deferred to a later EDT tick, so the realized `isVisible` lags the
    // request; this guards against scheduling the same transition again on the recompositions that
    // happen before the deferred runnable executes, and each deferred runnable applies only while it
    // still carries this latest request. Cleared when the dialog leaves the composition, which
    // retires any still-queued transition.
    val requestedVisible = remember { arrayOfNulls<Boolean>(1) }

    CompositionOwnedWindowHost(
        peer = dialog,
        onCloseRequest = onCloseRequest,
        title = title,
        resizable = resizable,
        position = state.position,
        width = state.width,
        height = state.height,
        setPosition = { state.position = it },
        setSize = { width, height ->
            state.width = width
            state.height = height
        },
        appliedGeometry = appliedGeometry,
        installExtras = {
            // A dialog is not a frame and carries no extended state, so no extended-state write-back
            // listener is installed; the only teardown owed is retiring a still-queued visibility
            // transition, whose apply to the disposed dialog would realize a fresh peer with no content
            // or listeners.
            val retireQueuedShow = { requestedVisible[0] = null }
            retireQueuedShow
        },
        applyExtras = {
            if (requestedVisible[0] != visible) {
                requestedVisible[0] = visible
                // Defer the show to a fresh EDT tick: a modal setVisible(true) blocks inside a nested
                // secondary event loop until the dialog hides, so it must run on a plain runnable rather
                // than inline in this effect body. The parent recomposer's continuations and frame-clock
                // timer firings are pumped by that same loop and keep recomposing the dialog content while
                // it shows. By the time the runnable runs, a newer request or a disposal may have retired
                // this one; a retired transition must not touch the peer.
                SwingUtilities.invokeLater {
                    if (requestedVisible[0] == visible) dialog.isVisible = visible
                }
            }
        },
        disposePeer = {
            // Hide before releasing the peer so a show that is already executing (a modal show blocks
            // inside a nested event loop, which also pumps this disposal) returns cleanly.
            dialog.isVisible = false
            dialog.dispose()
        },
        content = content,
    )
}
