package org.jetbrains.compose.swing.modifier.datatransfer

import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.Transferable
import java.awt.event.InputEvent
import javax.swing.JComponent

/*
 * Shared internals backing the data-transfer SwingModifiers: the single transfer handler installed per
 * component and the slice-ownership helpers that let drag, drop, and clipboard coexist on it.
 */

internal fun exportToClipboard(
    component: JComponent,
    action: Int,
) {
    val handler = component.transferHandler ?: return
    val clipboard = systemClipboard ?: return
    handler.exportToClipboard(component, clipboard, action)
}

/**
 * The system clipboard, or `null` in a headless environment that has none.
 *
 * A missing clipboard is the one expected, benign failure here and resolves to `null` so clipboard
 * modifiers become inert rather than crashing on headless setups. Any other failure (e.g. a denied
 * security check) is left to propagate — it signals a real misconfiguration the caller should see.
 */
internal val systemClipboard: Clipboard?
    get() =
        try {
            Toolkit.getDefaultToolkit().systemClipboard
        } catch (_: HeadlessException) {
            null
        }

/**
 * The current system-clipboard contents, or `null` when there is no clipboard (headless) or it is
 * momentarily unavailable.
 *
 * A clipboard owned/locked by another application throws [IllegalStateException] from
 * [Clipboard.getContents], and a sandbox that denies clipboard reads throws [SecurityException]; both
 * are expected conditions for which the right behavior is to read nothing (so a paste becomes a no-op)
 * rather than to fail. Any other failure is left to propagate.
 */
internal fun clipboardContents(): Transferable? {
    val clipboard = systemClipboard ?: return null
    return try {
        clipboard.getContents(null)
    } catch (_: IllegalStateException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

/**
 * The platform menu-shortcut modifier for clipboard key bindings: Command (META) on macOS, Control
 * elsewhere.
 */
internal fun menuShortcutMask(): Int {
    val mac = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)
    return if (mac) InputEvent.META_DOWN_MASK else InputEvent.CTRL_DOWN_MASK
}

/**
 * Returns the [SharedTransferHandler] installed on [component], installing one on first need. The
 * single handler every data-transfer modifier configures.
 */
internal fun installedHandler(component: JComponent): SharedTransferHandler {
    val existing = component.transferHandler
    if (existing is SharedTransferHandler) return existing
    val handler = SharedTransferHandler()
    handler.original = existing
    component.transferHandler = handler
    return handler
}

/** Drops [component]'s [SharedTransferHandler] once no capability remains, restoring its original. */
internal fun uninstallIfEmpty(component: JComponent) {
    val handler = component.transferHandler as? SharedTransferHandler ?: return
    if (handler.source == null && handler.drop == null) {
        component.transferHandler = handler.original
    }
}

/** Clears the handler's source slice only if [token] still owns it — never another element's. */
internal fun clearSourceIfOwned(
    component: JComponent,
    token: SliceToken?,
) {
    val handler = component.transferHandler as? SharedTransferHandler ?: return
    if (token != null) handler.clearSource(token)
}

/** Clears the handler's drop slice only if [token] still owns it — never another element's. */
internal fun clearDropIfOwned(
    component: JComponent,
    token: SliceToken?,
) {
    val handler = component.transferHandler as? SharedTransferHandler ?: return
    if (token != null) handler.clearDrop(token)
}
