package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.Slider
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.background
import org.jetbrains.compose.swing.modifier.appearance.opaque
import org.jetbrains.compose.swing.modifier.interaction.onFocus
import org.jetbrains.compose.swing.modifier.interaction.onHover
import org.jetbrains.compose.swing.modifier.interaction.onPointerEvent
import org.jetbrains.compose.swing.modifier.keyboard.onKeyEvent
import org.jetbrains.compose.swing.modifier.keyboard.onKeyStroke
import org.jetbrains.compose.swing.modifier.layout.alignmentX
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import org.jetbrains.compose.swing.modifier.listener.actionListener
import org.jetbrains.compose.swing.modifier.listener.changeListener
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.event.ChangeListener

// The interaction half of the modifier gallery: the input-listener, keyboard, and raw-listener cards,
// split from the appearance/layout cards so each file stays focused on one modifier family.

@Composable
internal fun HoverFocusCard() {
    ExampleCard("onHover / onFocus") {
        var status by remember { mutableStateOf("idle") }
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Button(
                "Hover or focus me",
                modifier =
                    SwingModifier
                        .onHover(
                            onEnter = { status = "hovering" },
                            onExit = { status = "idle" },
                        ).onFocus(
                            onGained = { status = "focused" },
                            onLost = { status = "blurred" },
                        ),
            )
        }
        Label("Status: $status")
    }
}

@Composable
internal fun PointerCard() {
    ExampleCard("onPointerEvent") {
        var lastEvent by remember { mutableStateOf("none") }
        Label(
            "Click anywhere on this label",
            modifier =
                SwingModifier
                    .opaque(true)
                    .background(Color(0xE3, 0xF2, 0xFD))
                    .preferredSize(Dimension(260, 32))
                    .alignmentX(LEFT_ALIGNED)
                    .onPointerEvent(
                        onPress = { e -> lastEvent = "pressed button ${e.button}" },
                        onClick = { e -> lastEvent = "clicked x${e.clickCount} button ${e.button}" },
                    ),
        )
        Label("Last pointer event: $lastEvent")
    }
}

@Composable
internal fun KeyStrokeCard() {
    ExampleCard("onKeyStroke") {
        var saves by remember { mutableIntStateOf(0) }
        var text by remember { mutableStateOf("Focus me, press ctrl S") }
        TextField(
            value = text,
            modifier = SwingModifier.onKeyStroke("ctrl S") { saves++ },
            onValueChange = { text = it },
            columns = 28,
        )
        Label("Save shortcut fired $saves time(s)")
    }
}

@Composable
internal fun KeyEventCard() {
    ExampleCard("onKeyEvent") {
        var consume by remember { mutableStateOf(false) }
        var lastKey by remember { mutableStateOf("none") }
        var text by remember { mutableStateOf("Type here") }
        CheckBox(text = "Consume key events", checked = consume, onCheckedChange = { consume = it })
        TextField(
            value = text,
            modifier =
                SwingModifier.onKeyEvent { e ->
                    if (e.id == KeyEvent.KEY_PRESSED) lastKey = KeyEvent.getKeyText(e.keyCode)
                    consume
                },
            onValueChange = { text = it },
            columns = 28,
        )
        Label("Last key pressed: $lastKey")
    }
}

@Composable
internal fun SwingListenerCard() {
    ExampleCard("actionListener (raw ActionListener)") {
        var rawClicks by remember { mutableIntStateOf(0) }
        // remember the listener instance: Swing listeners are additive, so a fresh instance each
        // recomposition would detach the old and attach the new. The wrapper's onClick still fires too.
        val listener = remember { ActionListener { rawClicks++ } }
        Button(
            "Click (raw listener)",
            modifier = SwingModifier.actionListener(listener),
        )
        Label("Raw listener fired $rawClicks time(s)")
    }
}

@Composable
internal fun ChangeListenerCard() {
    ExampleCard("changeListener (raw ChangeListener)") {
        var value by remember { mutableIntStateOf(0) }
        var changes by remember { mutableIntStateOf(0) }
        val listener = remember { ChangeListener { changes++ } }
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Label("Value: $value")
            Slider(
                value = value,
                onValueChange = { value = it },
                min = 0,
                max = 100,
                modifier = SwingModifier.changeListener(listener),
            )
        }
        Label("Raw change listener fired $changes time(s)")
    }
}
