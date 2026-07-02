package org.jetbrains.compose.swing.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.constants.WindowExtendedState
import java.awt.Dimension
import java.awt.Frame

/**
 * Creates a [WindowState] that is remembered across compositions.
 *
 * Changes to the provided initial values after the state has been created do **not** recreate or
 * mutate it; hoist the state and update its properties directly to drive the window afterwards.
 *
 * @param position the initial value for [WindowState.position]
 * @param size the initial value for [WindowState.size]; a null size sizes the window to its content,
 *   while a non-null size is applied verbatim
 * @param extendedState the initial value for [WindowState.extendedState]
 */
@Composable
public fun rememberWindowState(
    position: WindowPosition = WindowPosition.PlatformDefault,
    size: Dimension? = null,
    @WindowExtendedState extendedState: Int = Frame.NORMAL,
): WindowState = remember { WindowState(position, size, extendedState) }

/**
 * A state object that can be hoisted to control and observe a [Window]'s geometry and extended state.
 *
 * @param position the initial value for [WindowState.position]
 * @param size the initial value for [WindowState.size]; a null size sizes the window to its content,
 *   while a non-null size is applied verbatim
 * @param extendedState the initial value for [WindowState.extendedState]
 */
public fun WindowState(
    position: WindowPosition = WindowPosition.PlatformDefault,
    size: Dimension? = null,
    @WindowExtendedState extendedState: Int = Frame.NORMAL,
): WindowState = WindowStateImpl(position, size?.width ?: 0, size?.height ?: 0, extendedState)

/**
 * A state object that can be hoisted to control and observe a [Window]'s geometry and extended state.
 *
 * Every property is two-way: assigning to one repositions, resizes, maximizes, minimizes or restores
 * the realized window, and a user driving the same change through the window system writes the new
 * value back into the state.
 */
public interface WindowState {
    /** The current top-left position of the window on screen. */
    public var position: WindowPosition

    /** The current width of the window, in pixels. */
    public var width: Int

    /** The current height of the window, in pixels. */
    public var height: Int

    /**
     * The current size of the window, a [width]/[height] pair in pixels.
     *
     * Reading returns a detached copy, matching [java.awt.Component.getSize] semantics; resize the
     * window by assigning a new value here or by setting [width] and [height] individually.
     */
    public var size: Dimension
        get() = Dimension(width, height)
        set(value) {
            width = value.width
            height = value.height
        }

    /**
     * The current extended state of the window, a [WindowExtendedState] constant:
     * [Frame.MAXIMIZED_BOTH] maximizes the window, [Frame.ICONIFIED] minimizes it, and
     * [Frame.NORMAL] restores it.
     */
    @WindowExtendedState
    public var extendedState: Int
}

private class WindowStateImpl(
    position: WindowPosition,
    width: Int,
    height: Int,
    extendedState: Int,
) : WindowState {
    override var position by mutableStateOf(position)
    override var width by mutableIntStateOf(width)
    override var height by mutableIntStateOf(height)
    override var extendedState by mutableIntStateOf(extendedState)
}
