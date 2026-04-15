package org.jetbrains.compose.swing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Updater
import java.awt.Component

@Composable
public inline fun <T : Component> SwingNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
) {
    ComposeNode<T, SwingApplier>(factory, update)
}

@Composable
public inline fun <T : Component> SwingNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    content: @Composable @SwingComposable () -> Unit,
) {
    ComposeNode<T, SwingApplier>(factory, update, content)
}
