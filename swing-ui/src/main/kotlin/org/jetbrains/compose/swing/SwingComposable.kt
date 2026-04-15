package org.jetbrains.compose.swing

import androidx.compose.runtime.ComposableTargetMarker

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "Swing Composable")
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
public annotation class SwingComposable
