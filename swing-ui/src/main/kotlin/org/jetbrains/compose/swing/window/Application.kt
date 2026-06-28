package org.jetbrains.compose.swing.window

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.jetbrains.compose.swing.core.GlobalSnapshotManager
import org.jetbrains.compose.swing.core.SwingFrameClock
import kotlin.coroutines.coroutineContext
import kotlin.system.exitProcess

/**
 * An entry point for the Compose application. See [awaitApplication] for more information.
 *
 * Usually this entry point is used inside `main()` function:
 * ```
 * fun main() = application {
 *
 * }
 * ```
 *
 * After all windows are closed and all operations are completed, the application will end.
 * Set [exitProcessOnExit] to `false`, if you need to execute some code after [application] block,
 * otherwise the code after it won't be executed, as [application] will exit the process.
 *
 * This entry point is a blocking operation (it blocks the current thread until application
 * finishes) and can't be called inside UI thread. To launch new application from UI thread (for
 * example, from some event listener), use `GlobalScope.launchApplication` instead.
 *
 * Application can launch background tasks using [LaunchedEffect]
 * or create [Window], [DialogWindow], or [Tray] in a declarative Compose way:
 *
 * ```
 * fun main() = application {
 *     var isSplashScreenShowing by remember { mutableStateOf(true) }
 *
 *     LaunchedEffect(Unit) {
 *         delay(2000)
 *         isSplashScreenShowing = false
 *     }
 *
 *     if (isSplashScreenShowing) {
 *         Window(::exitApplication, title = "Splash") {}
 *     } else {
 *         Window(::exitApplication, title = "App") {}
 *     }
 * }
 * ```
 *
 * When there is no any active compositions, this function will end.
 * Active composition is a composition that have active coroutine (for example, launched in
 * [LaunchedEffect]) or that have child composition created inside [Window], [DialogWindow], or [Tray].
 *
 * @param exitProcessOnExit should `exitProcess(0)` be called after the application is closed.
 * exitProcess speedup process exit (instant instead of 1-4sec).
 * If `false`, the execution of the function will be unblocked after application is exited
 * (when the last window is closed, and all [LaunchedEffect] are complete).
 * @see [awaitApplication]
 */
public fun application(
    exitProcessOnExit: Boolean = true,
    content: @Composable ApplicationScope.() -> Unit,
) {
    runBlocking {
        awaitApplication {
            content()
        }
    }

    if (exitProcessOnExit) {
        exitProcess(0)
    }
}

/**
 * Short variant of launching application inside [CoroutineScope].
 *
 * This function is equivalent of:
 * ```
 * CoroutineScope.launch {
 *     awaitApplication {
 *
 *     }
 * }
 * ```
 *
 * Don't use `GlobalScope.launchApplication {}` to launch application inside `main()` function
 * without waiting it to end: it does not block the main thread, so the application process may stop
 * before any window appears.
 *
 * @see [awaitApplication]
 */
public fun CoroutineScope.launchApplication(content: @Composable ApplicationScope.() -> Unit): Job =
    launch {
        awaitApplication(content = content)
    }

/**
 * An entry point for the Compose application.
 *
 * Application can launch background tasks using [LaunchedEffect]
 * or create [Window], [DialogWindow], or [Tray] in a declarative Compose way:
 *
 * ```
 * fun main() = runBlocking {
 *     awaitApplication {
 *         var isSplashScreenShowing by remember { mutableStateOf(true) }
 *
 *         LaunchedEffect(Unit) {
 *             delay(2000)
 *             isSplashScreenShowing = false
 *         }
 *
 *         if (isSplashScreenShowing) {
 *             Window(::exitApplication, title = "Splash") {}
 *         } else {
 *             Window(::exitApplication, title = "App") {}
 *         }
 *     }
 * }
 * ```
 *
 * When there is no any active compositions, this function will end.
 * Active composition is a composition that have active coroutine (for example, launched in
 * [LaunchedEffect]) or that have child composition created inside [Window], [DialogWindow], or [Tray].
 *
 * Don't run animations directly in this function
 * (for example, [withFrameNanos] or [androidx.compose.animation.core.animateFloatAsState]):
 * outside a window, frames are produced as fast as possible rather than at a display refresh rate.
 *
 * All animation's should be created inside Composable content of the
 * [Window] / [DialogWindow] / [ComposePanel].
 *
 * [Window] and [Dialog] created inside [content] run as part of this application composition:
 * application-scope state and any [androidx.compose.runtime.CompositionLocal] provided in [content]
 * flow into their content.
 */
public suspend fun awaitApplication(content: @Composable ApplicationScope.() -> Unit) {
    val frameClock = SwingFrameClock()
    try {
        withContext(Dispatchers.Swing) {
            withContext(frameClock) {
                GlobalSnapshotManager.ensureStarted()

                val recomposer = Recomposer(coroutineContext)
                var isOpen by mutableStateOf(true)

                val applicationScope =
                    object : ApplicationScope {
                        override fun exitApplication() {
                            isOpen = false
                        }
                    }

                launch {
                    recomposer.runRecomposeAndApplyChanges()
                }

                launch {
                    val applier = ApplicationApplier()
                    val composition = Composition(applier, recomposer)
                    try {
                        composition.setContent {
                            if (isOpen) {
                                applicationScope.content()
                            }
                        }
                        recomposer.close()
                        recomposer.join()
                    } finally {
                        composition.dispose()
                    }
                }
            }
        }
    } finally {
        frameClock.dispose()
    }
}

/**
 * Scope used by [application], [awaitApplication], [launchApplication]
 */
@Stable
public interface ApplicationScope {
    /**
     * Close all windows created inside the application and cancel all launched effects
     * (they launch via [LaunchedEffect] and [rememberCoroutineScope]).
     */
    public fun exitApplication()
}

private class ApplicationApplier : Applier<Any> {
    override val current: Any = Unit

    override fun down(node: Any) = Unit

    override fun up() = Unit

    override fun insertTopDown(
        index: Int,
        instance: Any,
    ) {
        check(instance is Unit) {
            "Composable content may not be added directly into " +
                ApplicationScope::class.simpleName
        }
    }

    override fun insertBottomUp(
        index: Int,
        instance: Any,
    ) {
        check(instance is Unit) {
            "Composable content may not be added directly into " +
                ApplicationScope::class.simpleName
        }
    }

    override fun remove(
        index: Int,
        count: Int,
    ) = Unit

    override fun move(
        from: Int,
        to: Int,
        count: Int,
    ) = Unit

    override fun clear() = Unit

    override fun onEndChanges() = Unit
}
