package org.jetbrains.compose.swing.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.core.KeepEnclosingApplicationAlive
import org.jetbrains.compose.swing.setContentAsInteropHost
import java.awt.Dialog
import java.awt.Frame
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.RootPaneContainer

/**
 * Wires a composition-owned top-level AWT [peer] (a [javax.swing.JFrame] or [javax.swing.JDialog])
 * into the enclosing composition: it forwards the window-closing gesture to [onCloseRequest], installs
 * the geometry write-back, hosts [content] as a child of the enclosing composition, and pushes the
 * reactive [title]/[resizable]/geometry onto the peer.
 *
 * The shape of the two directions matches the two-way geometry model: [position]/[width]/[height] are
 * read here in the composable body (so mutating them recomposes and re-applies), while [setPosition]
 * and [setSize] carry user-driven moves and resizes back into the hoisted state.
 *
 * Everything that is specific to one kind of peer is threaded in as a lambda: [installExtras] registers
 * any additional listeners in the same [DisposableEffect] and returns their removal, [applyExtras] runs
 * at the tail of the reactive [SideEffect] (after geometry, so it can flip visibility once the peer is
 * sized and positioned), and [disposePeer] releases the peer at the very end of teardown so a caller can
 * interleave its own steps (for example, hiding a modal dialog before it is disposed).
 */
@Composable
internal fun CompositionOwnedWindowHost(
    peer: Window,
    onCloseRequest: () -> Unit,
    title: String,
    resizable: Boolean,
    position: WindowPosition,
    width: Int,
    height: Int,
    setPosition: (WindowPosition) -> Unit,
    setSize: (width: Int, height: Int) -> Unit,
    appliedGeometry: AppliedGeometry,
    installExtras: () -> () -> Unit,
    applyExtras: () -> Unit,
    disposePeer: () -> Unit,
    content: @Composable () -> Unit,
) {
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)
    val currentContent by rememberUpdatedState(content)

    KeepEnclosingApplicationAlive()

    // Capture the enclosing composition context (the application/window composition) here in the
    // composable body, NOT inside the DisposableEffect: the peer's content pane is a detached top-level
    // peer, so the Swing-tree walk from it finds no parent. Threading this context through explicitly
    // makes the peer content a CHILD of the enclosing composition, so app-scope state and
    // CompositionLocals flow into the content. This is the deliberate "preserve app->window flow"
    // choice: a window created declaratively under application { } stays a child of the enclosing
    // composition rather than spinning up its own window-local recomposer.
    val parentContext = rememberCompositionContext()

    val container = peer as RootPaneContainer

    DisposableEffect(Unit) {
        val windowListener =
            object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    currentOnCloseRequest()
                }
            }
        peer.addWindowListener(windowListener)

        val geometryListener =
            peer.installGeometryWriteBack(
                applied = appliedGeometry,
                setPosition = setPosition,
                setSize = setSize,
            )

        val removeExtras = installExtras()

        val handle =
            container.contentPane.setContentAsInteropHost(parentContext) {
                CompositionLocalProvider(LocalWindow provides peer) {
                    currentContent()
                }
            }

        onDispose {
            removeExtras()
            handle.dispose()
            peer.removeComponentListener(geometryListener)
            peer.removeWindowListener(windowListener)
            disposePeer()
        }
    }

    // Reactive params: re-applied whenever the corresponding argument changes across recomposition.
    // Effect bodies run on the composition's Swing dispatcher (the EDT), so these mutations are
    // EDT-safe. Geometry is applied before [applyExtras] so the peer is sized and positioned before its
    // visibility (and, for a frame, its extended state) is flipped.
    SideEffect {
        peer.applyChrome(title, resizable)
        peer.applyGeometry(position, width, height, appliedGeometry)
        applyExtras()
    }
}

/**
 * Pushes the declared [title] and [resizable] onto this peer when they differ from what it already
 * carries. Both [Frame] and [Dialog] declare these accessors independently, with no shared supertype,
 * so the write dispatches on the concrete peer type.
 */
private fun Window.applyChrome(
    title: String,
    resizable: Boolean,
) {
    when (this) {
        is Frame -> {
            if (this.title != title) this.title = title
            if (isResizable != resizable) isResizable = resizable
        }

        is Dialog -> {
            if (this.title != title) this.title = title
            if (isResizable != resizable) isResizable = resizable
        }
    }
}
