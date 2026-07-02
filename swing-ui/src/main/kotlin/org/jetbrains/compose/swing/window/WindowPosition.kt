package org.jetbrains.compose.swing.window

/**
 * Constructs a [WindowPosition.Absolute] from [x] and [y] pixel coordinates.
 */
public fun WindowPosition(
    x: Int,
    y: Int,
): WindowPosition = WindowPosition.Absolute(x, y)

/**
 * Top-left position of a window or dialog on the screen, in raw pixels.
 */
public sealed interface WindowPosition {
    /** The horizontal position of the window, in pixels. */
    public val x: Int

    /** The vertical position of the window, in pixels. */
    public val y: Int

    /**
     * `true` when the position has concrete screen coordinates, `false` when it is [PlatformDefault].
     */
    public val isSpecified: Boolean

    /**
     * The window has not been placed yet, so the platform positions it (typically in a cascade
     * relative to the previously focused window).
     *
     * [x] and [y] are `0` until the platform places the window.
     *
     * Meaningful only before the window is shown: once a window is visible it always has concrete
     * coordinates and can no longer return to [PlatformDefault].
     */
    public object PlatformDefault : WindowPosition {
        override val x: Int get() = 0
        override val y: Int get() = 0
        override val isSpecified: Boolean get() = false

        override fun toString(): String = "PlatformDefault"
    }

    /**
     * Absolute top-left position in pixels relative to the screen.
     */
    public data class Absolute(
        override val x: Int,
        override val y: Int,
    ) : WindowPosition {
        override val isSpecified: Boolean get() = true
    }
}
