@file:JvmMultifileClass
@file:JvmName("ComponentsKt")

package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.annotations.InternalSwingUiApi
import org.jetbrains.compose.swing.annotations.SwingMenuComposable
import org.jetbrains.compose.swing.core.MenuApplier
import org.jetbrains.compose.swing.core.SwingCompositionMount
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPopupMenu
import javax.swing.JWindow
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

/**
 * Registers a system-tray icon for the lifetime of this composition.
 *
 * The icon shows [image] with [tooltip] as its hover text. Activating the icon (the platform's primary
 * click) runs [onAction]. Requesting its popup (the platform popup gesture) opens a menu built from
 * [menu], the same menu tree used by a menu bar or context menu — `Menu`, `MenuItem`,
 * `CheckBoxMenuItem`, `RadioButtonMenuItem`, `MenuSeparator`. The menu reads composition state, so its
 * items reflect the current state each time it opens, and an item's callback updates state like any
 * other composable callback.
 *
 * The icon is added when this enters the composition and removed when it leaves. It does nothing when
 * the platform has no system tray.
 *
 * Call this inside an `application { }` content block so the menu shares the application composition's
 * scope and [androidx.compose.runtime.CompositionLocal]s.
 *
 * @param image the icon image shown in the tray.
 * @param tooltip the hover text for the icon.
 * @param onAction callback run when the icon is activated.
 * @param menu the composable menu tree opened on the popup gesture.
 */
@Composable
public fun Tray(
    image: Image,
    tooltip: String = "",
    onAction: () -> Unit = {},
    menu:
        @Composable @SwingMenuComposable
        () -> Unit = {},
) {
    if (!SystemTray.isSupported()) return

    val currentOnAction by rememberUpdatedState(onAction)
    val currentMenu by rememberUpdatedState(menu)
    val parentContext = rememberCompositionContext()

    val menuHost = remember { TrayMenuHost(parentContext) { currentMenu() } }
    val trayIcon = remember { TrayIcon(image).apply { isImageAutoSize = true } }

    SideEffect {
        if (trayIcon.image != image) trayIcon.image = image
        if (trayIcon.toolTip != tooltip) trayIcon.toolTip = tooltip
    }

    DisposableEffect(Unit) {
        val actionListener = ActionListener { currentOnAction() }
        val mouseListener =
            object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent): Unit = maybeShow(e)

                override fun mouseReleased(e: MouseEvent): Unit = maybeShow(e)

                private fun maybeShow(e: MouseEvent) {
                    // isPopupTrigger is set on press on some platforms and on release on others;
                    // checking both events is the cross-platform-correct gesture detection.
                    if (e.isPopupTrigger) menuHost.showMenu(e.x, e.y)
                }
            }
        trayIcon.addActionListener(actionListener)
        trayIcon.addMouseListener(mouseListener)

        val systemTray = SystemTray.getSystemTray()
        systemTray.add(trayIcon)
        onDispose {
            systemTray.remove(trayIcon)
            trayIcon.removeActionListener(actionListener)
            trayIcon.removeMouseListener(mouseListener)
        }
    }
}

/**
 * Builds and presents the popup menu of a [Tray]. On each [showMenu] call it composes [menu] fresh into
 * a [JPopupMenu] nested in [parentContext], so the menu reflects the current composition state, then
 * hands the populated popup to [display]. The menu composition is disposed when the popup closes.
 *
 * @param parentContext the composition context the popup menu nests into.
 * @param display presents the populated popup at the gesture point; the production default shows it
 *   over a transient invoker at those screen coordinates.
 * @param menu the composable menu tree opened on each [showMenu] call.
 */
@InternalSwingUiApi
public class TrayMenuHost(
    private val parentContext: CompositionContext,
    private val display: (popup: JPopupMenu, x: Int, y: Int) -> Unit = ::showPopupAtCursor,
    private val menu:
        @Composable @SwingMenuComposable
        () -> Unit,
) {
    /** Composes [menu] into a fresh [JPopupMenu] and presents it through [display] at ([x], [y]). */
    public fun showMenu(
        x: Int,
        y: Int,
    ) {
        val popup = JPopupMenu()
        val mount = SwingCompositionMount.nested(parentContext) { observer -> MenuApplier(popup, observer) }
        mount.setContent(menu)

        popup.addPopupMenuListener(
            object : PopupMenuListener {
                override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) = Unit

                override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
                    popup.removePopupMenuListener(this)
                    mount.dispose()
                }

                override fun popupMenuCanceled(e: PopupMenuEvent) = Unit
            },
        )

        display(popup, x, y)
    }
}

/**
 * Presents [popup] at screen coordinates ([x], [y]) over a transient, invisible invoker — the
 * production default for a tray popup, whose mouse events carry no Swing invoker of their own.
 */
private fun showPopupAtCursor(
    popup: JPopupMenu,
    x: Int,
    y: Int,
) {
    val invoker =
        JWindow().apply {
            setLocation(x, y)
            isVisible = true
        }
    popup.addPopupMenuListener(
        object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) = Unit

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
                popup.removePopupMenuListener(this)
                invoker.dispose()
            }

            override fun popupMenuCanceled(e: PopupMenuEvent) = Unit
        },
    )
    popup.show(invoker, 0, 0)
}
