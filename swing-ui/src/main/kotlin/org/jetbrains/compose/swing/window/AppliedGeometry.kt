package org.jetbrains.compose.swing.window

import org.jetbrains.compose.swing.constants.WindowExtendedState
import java.awt.Frame
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.WindowStateListener

/**
 * Records the geometry (and, for frames, the extended state) that is currently in sync between a
 * hoisted state and its realized AWT window.
 *
 * Both the state-to-window apply and the window-to-state write-back update this holder, which is what
 * keeps the two directions from fighting: the apply skips whenever the declared value already matches
 * what was last applied, so a resize or move that originated from the user (and was written straight
 * back here) is never re-applied on the next recomposition.
 */
internal class AppliedGeometry {
    var position: WindowPosition? = null
    var width: Int? = null
    var height: Int? = null
    var extendedState: Int? = null

    /**
     * Whether an unspecified (0x0) size has already been resolved to the content's preferred size by
     * [java.awt.Window.pack]. Guards against re-packing on later recompositions that observe the state
     * still at 0x0 before the pack's realized size has been written back.
     */
    var packed: Boolean = false
}

/**
 * Pushes the declared [position], [width] and [height] onto this window when they differ from what is
 * already in sync, updating [applied] to match. Runs on the event dispatch thread (the composition's
 * Swing dispatcher), so the AWT mutations are thread-safe.
 *
 * An unspecified size (a 0x0 [width] by [height]) sizes the window to its content's preferred size via
 * [java.awt.Window.pack] the first time it is applied; the realized size then flows back into the state
 * through the geometry write-back listener. A non-null size is applied verbatim through [setSize].
 *
 * An unspecified position ([WindowPosition.PlatformDefault]) is left to the platform: only a position
 * whose [WindowPosition.isSpecified] holds moves the window.
 */
internal fun Window.applyGeometry(
    position: WindowPosition,
    width: Int,
    height: Int,
    applied: AppliedGeometry,
) {
    if (width == 0 && height == 0) {
        if (!applied.packed) {
            pack()
            applied.packed = true
        }
    } else if (applied.width != width || applied.height != height) {
        setSize(width, height)
        applied.width = width
        applied.height = height
    }
    if (applied.position != position) {
        if (position.isSpecified) setLocation(position.x, position.y)
        applied.position = position
    }
}

/**
 * Pushes the declared [extendedState] onto this frame when it differs from what is already in sync,
 * updating [applied] to match. Runs on the event dispatch thread (the composition's Swing
 * dispatcher), so the AWT mutation is thread-safe.
 */
internal fun Frame.applyExtendedState(
    @WindowExtendedState extendedState: Int,
    applied: AppliedGeometry,
) {
    if (applied.extendedState != extendedState) {
        this.extendedState = extendedState
        applied.extendedState = extendedState
    }
}

/**
 * Registers a listener that writes user-driven maximize, minimize and restore transitions of this
 * frame back through [setExtendedState], keeping [applied] equal to the value it hands to the state.
 * Stamping [applied] here closes the feedback loop the same way [installGeometryWriteBack] does for
 * moves and resizes.
 *
 * Returns the registered listener so the caller can remove it when the window leaves the composition.
 */
internal fun Frame.installExtendedStateWriteBack(
    applied: AppliedGeometry,
    setExtendedState: (Int) -> Unit,
): WindowStateListener {
    val listener =
        WindowStateListener { event ->
            val newState = event.newState
            // A transition whose result already matches what was last applied is an echo of our own
            // apply (or a stale event); leaving the state untouched keeps a declared change from
            // being reverted before the next apply observes it.
            if (applied.extendedState != newState) {
                applied.extendedState = newState
                setExtendedState(newState)
            }
        }
    addWindowStateListener(listener)
    return listener
}

/**
 * Registers a listener that writes user-driven resizes and moves of this window back through [setSize]
 * and [setPosition], keeping [applied] equal to the value it hands to the state. Stamping [applied]
 * here is what closes the feedback loop: the next apply sees the state and [applied] already agree and
 * does nothing.
 *
 * Returns the registered listener so the caller can remove it when the window leaves the composition.
 */
internal fun Window.installGeometryWriteBack(
    applied: AppliedGeometry,
    setPosition: (WindowPosition) -> Unit,
    setSize: (width: Int, height: Int) -> Unit,
): ComponentListener {
    val listener =
        object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                val newWidth = width
                val newHeight = height
                // A resize whose result already matches what was last applied is an echo of our own
                // apply (or a stale event); leaving the state untouched keeps a declared change from
                // being reverted before the next apply observes it.
                if (applied.width == newWidth && applied.height == newHeight) return
                applied.width = newWidth
                applied.height = newHeight
                setSize(newWidth, newHeight)
            }

            override fun componentMoved(e: ComponentEvent) {
                val newPosition = WindowPosition.Absolute(x, y)
                // A move whose result already matches what was last applied is an echo of our own
                // apply (or a stale event); leaving the state untouched keeps a declared change from
                // being reverted before the next apply observes it.
                if (applied.position == newPosition) return
                applied.position = newPosition
                setPosition(newPosition)
            }
        }
    addComponentListener(listener)
    return listener
}
