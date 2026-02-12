package org.jetbrains.compose.swing

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.awt.event.ActionListener
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.coroutines.CoroutineContext

/**
 * Dispatcher for Swing event dispatching thread.
 *
 * Copy of Dispatchers.Swing from kotlinx-coroutines-swing.
 *
 * We don't depend on kotlinx-coroutines-swing, because it will override Dispatchers.Main, and
 * application can require a different Dispatchers.Main.
 *
 * Note, that we use internal API `Delay` and experimental `resumeUndispatched`.
 * That means it can be changed in the future. When it happens, we need
 * to release a new version of Skiko.
 */
@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
internal object SwingDispatcher : CoroutineDispatcher(), Delay {
    override fun dispatch(context: CoroutineContext, block: Runnable): Unit = SwingUtilities.invokeLater(block)

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val timer = TimeUnit.MILLISECONDS.schedule(timeMillis) {
            with(continuation) { resumeUndispatched(Unit) }
        }
        continuation.invokeOnCancellation { timer.stop() }
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val timer = TimeUnit.MILLISECONDS.schedule(timeMillis) {
            block.run()
        }
        return object : DisposableHandle {
            override fun dispose() {
                timer.stop()
            }
        }
    }

    private fun TimeUnit.schedule(time: Long, action: ActionListener): Timer =
        Timer(toMillis(time).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), action).apply {
            this.isRepeats = false
            start()
        }
}
