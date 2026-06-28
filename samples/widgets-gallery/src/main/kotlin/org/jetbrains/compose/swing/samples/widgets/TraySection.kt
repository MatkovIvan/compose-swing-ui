package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.CheckBoxMenuItem
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.MenuItem
import org.jetbrains.compose.swing.components.MenuSeparator
import org.jetbrains.compose.swing.components.Tray
import org.jetbrains.compose.swing.components.button.ToggleButton
import org.jetbrains.compose.swing.components.layout.FlowPanel
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage

/**
 * Demonstrates [Tray]: a system-tray icon mounted only while a toggle is on. The icon's activation and
 * menu callbacks write back into composition state, so the status labels echo the latest interaction.
 */
@Composable
internal fun TraySection() {
    SectionColumn {
        SectionHeading("System tray")
        TrayToggleCard()
    }
}

/**
 * A "Show tray icon" toggle gates the [Tray]. The icon is emitted only while the toggle is on, so the
 * section has no tray side effect on entry. Its [Tray.onAction] and a menu checkbox feed status labels.
 */
@Composable
private fun TrayToggleCard() {
    ExampleCard("Tray (button-gated)") {
        var showTray by remember { mutableStateOf(false) }
        var lastAction by remember { mutableStateOf("none") }
        var notificationsOn by remember { mutableStateOf(true) }

        ToggleButton(
            text = "Show tray icon",
            pressed = showTray,
            onPressedChange = { showTray = it },
        )
        FlowPanel {
            Label("Tray icon: ${if (showTray) "shown" else "hidden"}")
        }
        Label("Last action: $lastAction")
        Label("Notifications: ${if (notificationsOn) "on" else "off"}")

        if (showTray) {
            Tray(
                image = trayImage(),
                tooltip = "Compose Swing showcase",
                onAction = { lastAction = "icon activated" },
                menu = {
                    CheckBoxMenuItem(
                        text = "Notifications",
                        checked = notificationsOn,
                        onCheckedChange = { notificationsOn = it },
                    )
                    MenuSeparator()
                    MenuItem("Quit", onClick = { lastAction = "quit selected" })
                },
            )
        }
    }
}

private const val TRAY_ICON_SIZE = 16
private const val TRAY_ICON_CORNER = 6
private val TrayIconColor = Color(0x2D, 0x4B, 0x73)

/** A small solid-color icon generated for the tray; avoids shipping an image asset for the demo. */
private fun trayImage(): Image {
    val image = BufferedImage(TRAY_ICON_SIZE, TRAY_ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        graphics.color = TrayIconColor
        graphics.fillRoundRect(0, 0, TRAY_ICON_SIZE, TRAY_ICON_SIZE, TRAY_ICON_CORNER, TRAY_ICON_CORNER)
    } finally {
        graphics.dispose()
    }
    return image
}
