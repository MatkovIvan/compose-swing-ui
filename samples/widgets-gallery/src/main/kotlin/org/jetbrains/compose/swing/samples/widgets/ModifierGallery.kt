package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.layout.BoxPanel
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.background
import org.jetbrains.compose.swing.modifier.appearance.border
import org.jetbrains.compose.swing.modifier.appearance.clientProperty
import org.jetbrains.compose.swing.modifier.appearance.cursor
import org.jetbrains.compose.swing.modifier.appearance.font
import org.jetbrains.compose.swing.modifier.appearance.foreground
import org.jetbrains.compose.swing.modifier.appearance.opaque
import org.jetbrains.compose.swing.modifier.appearance.toolTip
import org.jetbrains.compose.swing.modifier.interaction.enabled
import org.jetbrains.compose.swing.modifier.interaction.focusable
import org.jetbrains.compose.swing.modifier.layout.alignmentX
import org.jetbrains.compose.swing.modifier.layout.alignmentY
import org.jetbrains.compose.swing.modifier.layout.maximumSize
import org.jetbrains.compose.swing.modifier.layout.minimumSize
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import org.jetbrains.compose.swing.modifier.layout.visible
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout

/**
 * A gallery where each [SwingModifier] builder visibly affects a real widget. State is hoisted so the
 * appearance modifiers toggle, the interaction modifiers (`onHover`/`onFocus`/`onPointerEvent`) update
 * a live status label, and the keyboard/raw-listener modifiers fire counters.
 *
 * The cards cover a representative set across the modifier families — appearance, layout, interaction,
 * keyboard, and raw listeners — each builder shown against a live widget. Data-transfer, context-menu,
 * and accessibility modifiers have their own dedicated sections.
 */
@Composable
internal fun ModifierGallery() {
    SectionColumn {
        SectionHeading("Modifier gallery")
        AppearanceCard()
        CursorAndToolTipCard()
        ClientPropertyCard()
        SizeAndVisibilityCard()
        SizeConstraintsCard()
        AlignmentCard()
        EnabledCard()
        FocusableCard()
        HoverFocusCard()
        PointerCard()
        KeyStrokeCard()
        KeyEventCard()
        SwingListenerCard()
        ChangeListenerCard()
    }
}

/** background + opaque + foreground + font + border, all on one label, toggled live. */
@Composable
private fun AppearanceCard() {
    ExampleCard("background / foreground / font / border") {
        var fancy by remember { mutableStateOf(true) }
        CheckBox(text = "Fancy styling", checked = fancy, onCheckedChange = { fancy = it })
        val styled =
            if (fancy) {
                SwingModifier
                    .opaque(true)
                    .background(Color(0xFF, 0xF3, 0xE0))
                    .foreground(Color(0xBF, 0x36, 0x0C))
                    .font(Font(Font.SERIF, Font.BOLD or Font.ITALIC, STYLED_FONT_SIZE))
                    .border(BorderFactory.createLineBorder(Color(0xBF, 0x36, 0x0C), 2))
            } else {
                SwingModifier
            }
        Label("Styled when the box is checked", modifier = styled)
    }
}

/** preferredSize on a button, and visible toggling another. */
@Composable
private fun SizeAndVisibilityCard() {
    ExampleCard("preferredSize / visible") {
        var shown by remember { mutableStateOf(true) }
        CheckBox(text = "Show the second button", checked = shown, onCheckedChange = { shown = it })
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Button("Wide button", modifier = SwingModifier.preferredSize(Dimension(WIDE_BUTTON, BUTTON_HEIGHT)))
            // The toggled button lives in a slot whose footprint is reserved by the wrapping panel's
            // explicit preferredSize. visible(false) hides the button but keeps it attached, so the
            // row no longer collapses and the checkbox above stays anchored. That is exactly what
            // visible() is for — unlike conditional composition (`if (shown) Button(...)`), which
            // would drop the child entirely and let the surrounding FlowLayout reflow.
            FlowPanel(
                modifier = SwingModifier.preferredSize(Dimension(TOGGLE_SLOT, BUTTON_HEIGHT)),
                hgap = 0,
                vgap = 0,
            ) {
                Button("Toggle me", modifier = SwingModifier.visible(shown))
            }
        }
    }
}

/** enabled on a field, controlled by a checkbox. */
@Composable
private fun EnabledCard() {
    ExampleCard("enabled") {
        var editable by remember { mutableStateOf(true) }
        var text by remember { mutableStateOf("Editable when enabled") }
        CheckBox(text = "Field enabled", checked = editable, onCheckedChange = { editable = it })
        TextField(
            value = text,
            modifier = SwingModifier.enabled(editable),
            onValueChange = { text = it },
            columns = 28,
        )
    }
}

/** cursor swaps the pointer over a target; toolTip attaches hover help text — both toggled live. */
@Composable
private fun CursorAndToolTipCard() {
    ExampleCard("cursor / toolTip") {
        var hand by remember { mutableStateOf(true) }
        CheckBox(text = "Hand cursor", checked = hand, onCheckedChange = { hand = it })
        Label(
            "Hover me: cursor + tooltip",
            modifier =
                SwingModifier
                    .opaque(true)
                    .background(Color(0xE8, 0xF5, 0xE9))
                    .preferredSize(Dimension(POINTER_TARGET, POINTER_HEIGHT))
                    .alignmentX(LEFT_ALIGNED)
                    .cursor(Cursor.getPredefinedCursor(if (hand) Cursor.HAND_CURSOR else Cursor.DEFAULT_CURSOR))
                    .toolTip(if (hand) "Click affordance: the hand cursor" else "Plain pointer"),
        )
    }
}

/** clientProperty stores an arbitrary key/value on the component; here it is set from the buttons. */
@Composable
private fun ClientPropertyCard() {
    ExampleCard("clientProperty") {
        var tag by remember { mutableStateOf("alpha") }
        Label(
            "Carries clientProperty \"role\" = $tag",
            modifier = SwingModifier.clientProperty("role", tag).alignmentX(LEFT_ALIGNED),
        )
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Button("role = alpha", onClick = { tag = "alpha" })
            Button("role = beta", onClick = { tag = "beta" })
        }
    }
}

/** minimumSize and maximumSize clamp a button between two bounds inside a resizing parent. */
@Composable
private fun SizeConstraintsCard() {
    ExampleCard("minimumSize / maximumSize") {
        WrappedCaption(
            "The button is clamped: a vertical BoxLayout would stretch it to the column width, but its " +
                "maximumSize caps the width while minimumSize keeps it from collapsing.",
        )
        Button(
            "Clamped button",
            modifier =
                SwingModifier
                    .minimumSize(Dimension(CLAMP_MIN, BUTTON_HEIGHT))
                    .maximumSize(Dimension(CLAMP_MAX, BUTTON_HEIGHT))
                    .alignmentX(LEFT_ALIGNED),
        )
    }
}

/** alignmentX/alignmentY position a child within a layout that honors alignment. */
@Composable
private fun AlignmentCard() {
    ExampleCard("alignmentX / alignmentY") {
        var aligned by remember { mutableStateOf(true) }
        CheckBox(text = "Align both rows left", checked = aligned, onCheckedChange = { aligned = it })
        // A vertical BoxLayout honors alignmentX: 0.0 lines both buttons up on the left edge; 0.5 centers
        // the narrow one over the wide one, so the offset is visible the moment the box is unchecked.
        BoxPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED), axis = BoxLayout.Y_AXIS) {
            Button(
                "Wide row button",
                modifier = SwingModifier.preferredSize(Dimension(WIDE_BUTTON, BUTTON_HEIGHT)),
            )
            Button(
                "Narrow",
                modifier = SwingModifier.alignmentX(if (aligned) LEFT_ALIGNED else CENTER_ALIGNED),
            )
        }
        // alignmentY governs how a child lines up against taller siblings in a horizontal BoxLayout.
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Label("Tall ↕", modifier = SwingModifier.preferredSize(Dimension(TALL_WIDTH, TALL_HEIGHT)))
            Label("top", modifier = SwingModifier.alignmentY(TOP_ALIGNED))
            Label("bottom", modifier = SwingModifier.alignmentY(BOTTOM_ALIGNED))
        }
    }
}

/** focusable toggles whether the control accepts keyboard focus at all. */
@Composable
private fun FocusableCard() {
    ExampleCard("focusable") {
        var canFocus by remember { mutableStateOf(true) }
        CheckBox(text = "Button is focusable", checked = canFocus, onCheckedChange = { canFocus = it })
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Button("Tab reaches me only when focusable", modifier = SwingModifier.focusable(canFocus))
        }
    }
}

private const val STYLED_FONT_SIZE = 15
private const val WIDE_BUTTON = 200
private const val BUTTON_HEIGHT = 30
private const val TOGGLE_SLOT = 120
private const val POINTER_TARGET = 260
private const val POINTER_HEIGHT = 32
private const val CLAMP_MIN = 120
private const val CLAMP_MAX = 240
private const val TALL_WIDTH = 60
private const val TALL_HEIGHT = 40
private const val CENTER_ALIGNED = 0.5f
private const val TOP_ALIGNED = 0.0f
private const val BOTTOM_ALIGNED = 1.0f
