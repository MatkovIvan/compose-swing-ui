package org.jetbrains.compose.swing.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javax.swing.JFrame
import javax.swing.WindowConstants

/**
 * Composes a Window (JFrame) with the given content.
 *
 * The window content runs as part of the enclosing application composition: state held in the
 * application scope and any [androidx.compose.runtime.CompositionLocal] provided above the window
 * flow into [content], and the content keeps recomposing while the window is shown.
 *
 * The [title], [visible] and [resizable] arguments are reactive: changing any of them in a
 * recomposition updates the realized window accordingly. Geometry and the extended state are driven
 * by [state], which is two-way: assigning to [WindowState.position]/[WindowState.size]/
 * [WindowState.extendedState] repositions, resizes, maximizes, minimizes or restores the window, and
 * a user driving the same change through the window system writes the new value back into [state].
 *
 * @param onCloseRequest callback to be called when the user attempts to close the window
 * @param state the hoistable, observable geometry (position and size) and extended state of the
 *   window
 * @param title the title of the window
 * @param visible whether the window should be visible
 * @param resizable whether the window can be resized
 * @param content the composable content of the window
 */
@Composable
public fun Window(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    title: String = "",
    visible: Boolean = true,
    resizable: Boolean = true,
    content: @Composable () -> Unit,
) {
    val frame =
        remember {
            JFrame().also { it.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE }
        }

    // Holds the geometry and extended state that are currently in sync between [state] and the realized
    // frame. Shared by the apply and the write-back listeners so the two directions never fight.
    val appliedGeometry = remember { AppliedGeometry() }

    val extendedState = state.extendedState

    CompositionOwnedWindowHost(
        peer = frame,
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
            val extendedStateListener =
                frame.installExtendedStateWriteBack(
                    applied = appliedGeometry,
                    setExtendedState = { state.extendedState = it },
                )
            val removeExtendedStateListener = { frame.removeWindowStateListener(extendedStateListener) }
            removeExtendedStateListener
        },
        applyExtras = {
            // The extended state is applied before the visibility flip so the window appears already
            // maximized, minimized or restored.
            frame.applyExtendedState(extendedState, appliedGeometry)
            if (frame.isVisible != visible) frame.isVisible = visible
        },
        disposePeer = { frame.dispose() },
        content = content,
    )
}
