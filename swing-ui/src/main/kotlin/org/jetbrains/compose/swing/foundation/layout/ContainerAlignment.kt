package org.jetbrains.compose.swing.foundation.layout

import java.awt.Component
import java.awt.Container

/**
 * What [alignment] reads for the first child of [target], or [Component.CENTER_ALIGNMENT] where the
 * container has no child to ask.
 *
 * A container built from this package reports its content's alignment rather than a fixed value of its own,
 * so the layout above it places it where it would have placed that content directly. A parent that lines
 * its children up on a shared alignment - `javax.swing.BoxLayout` - reserves room on both sides of that
 * line for every sibling, so a container answering a constant would sit off the line its content belongs
 * on and squeeze whichever siblings can stretch.
 *
 * A hidden child answers like any other, because these containers reserve its place as well: a container
 * whose reserved layout and whose reported alignment disagreed about which children exist would sit off
 * the line its own content was measured against.
 */
internal inline fun firstChildAlignment(
    target: Container,
    alignment: (Component) -> Float,
): Float {
    if (target.componentCount == 0) return Component.CENTER_ALIGNMENT
    return alignment(target.getComponent(0))
}
