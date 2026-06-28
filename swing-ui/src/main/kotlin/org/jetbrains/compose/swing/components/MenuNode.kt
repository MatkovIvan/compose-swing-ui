package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.ReusableComposeNode
import org.jetbrains.compose.swing.SwingNodeUpdater
import org.jetbrains.compose.swing.annotations.SwingMenuComposable
import org.jetbrains.compose.swing.core.MenuBarApplier
import org.jetbrains.compose.swing.core.SwingNodeHolder
import java.awt.Component

/**
 * Emits one menu node into the surrounding [MenuBarApplier] composition: [factory] builds the backing
 * Swing widget, [update] maps composition state onto it, and [content] supplies any nested menu items
 * (empty for a leaf such as a single item or separator).
 */
@Composable
@SwingMenuComposable
internal inline fun <reified T : Component> MenuNode(
    noinline factory: () -> T,
    crossinline update: @DisallowComposableCalls SwingNodeUpdater<T>.() -> Unit,
    content:
        @Composable @SwingMenuComposable
        () -> Unit = {},
) {
    ReusableComposeNode<SwingNodeHolder<T>, MenuBarApplier>(
        factory = { SwingNodeHolder(factory()) },
        update = { SwingNodeUpdater(this).update() },
        content = content,
    )
}
