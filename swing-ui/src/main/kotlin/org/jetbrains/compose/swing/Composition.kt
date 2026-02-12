package org.jetbrains.compose.swing

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.awt.Component
import java.awt.Container
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * Client property key for storing the composition associated with a component.
 */
internal const val COMPOSITION_KEY = "org.jetbrains.compose.swing.composition"

/**
 * A simple MonotonicFrameClock that yields to avoid blocking the UI thread.
 */
private object YieldFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(
        onFrame: (frameTimeNanos: Long) -> R
    ): R {
        yield()
        return onFrame(System.nanoTime())
    }
}

/**
 * Global Recomposer instance shared by all compositions.
 */
internal object GlobalRecomposer {
    private var instance: Recomposer? = null
    
    fun get(): Recomposer {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    GlobalSnapshotManager.ensureStarted()
                    val recomposer = Recomposer(SwingDispatcher + YieldFrameClock)
                    instance = recomposer
                    
                    // Start the recomposer
                    SwingUtilities.invokeLater {
                        CoroutineScope(SwingDispatcher + YieldFrameClock).launch {
                            recomposer.runRecomposeAndApplyChanges()
                        }
                    }
                }
            }
        }
        return instance!!
    }
}

/**
 * Sets the composable content of any Container (JPanel, JFrame.contentPane, etc.).
 * Uses a global Recomposer shared across all compositions.
 * 
 * This function will look for a parent composition in the Swing component tree
 * using client properties, allowing nested compositions to share the same recomposition scope.
 *
 * @param content the composable content to set
 */
fun Container.setContent(content: @Composable () -> Unit) {
    // Find parent composition by traversing the Swing tree
    val parentComposition = findParentComposition()
    
    val recomposer = GlobalRecomposer.get()
    val applier = SwingApplier(this)
    val composition = if (parentComposition != null) {
        Composition(applier, parentComposition)
    } else {
        Composition(applier, recomposer)
    }
    
    // Store the composition in client properties for child lookups (if this is a JComponent)
    if (this is JComponent) {
        putClientProperty(COMPOSITION_KEY, composition)
    }
    
    composition.setContent(content)
}

/**
 * Finds the parent composition by traversing up the Swing component tree.
 */
private fun Container.findParentComposition(): CompositionContext? {
    var current: Component? = parent
    while (current != null) {
        if (current is JComponent) {
            val composition = current.getClientProperty(COMPOSITION_KEY)
            if (composition is CompositionContext) {
                return composition
            }
        }
        current = current.parent
    }
    return null
}
