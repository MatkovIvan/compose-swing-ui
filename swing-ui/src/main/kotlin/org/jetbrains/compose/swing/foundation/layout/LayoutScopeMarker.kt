package org.jetbrains.compose.swing.foundation.layout

/**
 * Marks the scope a layout hands its content, so that only the innermost one is in reach.
 *
 * Without this marker an enclosing layout's scope stays a candidate inside a nested one, and a
 * declaration meant for the outer container is read by the inner container instead.
 */
@DslMarker
public annotation class LayoutScopeMarker
