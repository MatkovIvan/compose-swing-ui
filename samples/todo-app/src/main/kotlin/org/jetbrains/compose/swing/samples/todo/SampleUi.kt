package org.jetbrains.compose.swing.samples.todo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.components.layout.BoxPanel
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.border
import org.jetbrains.compose.swing.modifier.appearance.font
import org.jetbrains.compose.swing.modifier.appearance.foreground
import org.jetbrains.compose.swing.modifier.maximumSize
import org.jetbrains.compose.swing.modifier.minimumSize
import org.jetbrains.compose.swing.modifier.preferredSize
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.SwingConstants

/*
 * Shared layout primitives for the sample. They centralise spacing, typography, and the card shape so
 * the sample reads as one intentional design rather than a raw widget grid: consistent insets, a single
 * accent colour, and value labels that never reflow when the value they echo changes.
 */

private const val BODY_INSET = 16

/**
 * The whole screen, ready to compose into any host: it wraps the reactive list in a uniform body inset
 * so the content breathes inside the window. `main` composes exactly this — and so does the test, with
 * no window at all. Keeping it here, beside the shared layout primitives, leaves the `main` file free of
 * any `@Composable`.
 */
@Composable
internal fun ReactiveTaskListScreen() {
    BorderPanel(
        modifier =
            SwingModifier.border(
                BorderFactory.createEmptyBorder(BODY_INSET, BODY_INSET, BODY_INSET, BODY_INSET),
            ),
    ) {
        center { ReactiveTaskList() }
    }
}

/** The shared accent colour for headings and emphasis, kept neutral so it sits well in any host LaF. */
private val AccentColor = Color(0x2D, 0x4B, 0x73)

/** A muted colour for secondary, explanatory text. */
private val MutedColor = Color(0x5A, 0x5A, 0x5A)

/** Vertical gap between stacked rows inside a card. */
internal const val ROW_GAP: Int = 8

private const val TITLE_FONT_SIZE = 20
private const val HEADING_FONT_SIZE = 14

/** A sample's large title, shown once at the top of its body. */
@Composable
internal fun SampleTitle(text: String) {
    Label(
        text = text,
        modifier =
            SwingModifier
                .font(Font(Font.SANS_SERIF, Font.BOLD, TITLE_FONT_SIZE))
                .foreground(AccentColor),
        horizontalAlignment = SwingConstants.LEADING,
    )
}

/**
 * A short paragraph of secondary, explanatory text under a title or inside a card. The text is wrapped
 * in an HTML body with a bounded width so it flows onto multiple lines instead of forcing the layout
 * wide — long captions never trigger a horizontal scrollbar.
 */
@Composable
internal fun Caption(text: String) {
    Label(
        text = "<html><body style='width:${CAPTION_WRAP_WIDTH}px'>$text</body></html>",
        modifier =
            SwingModifier
                .font(Font(Font.SANS_SERIF, Font.PLAIN, HEADING_FONT_SIZE))
                .foreground(MutedColor),
        horizontalAlignment = SwingConstants.LEADING,
    )
}

private const val CAPTION_WRAP_WIDTH = 520

/**
 * A titled, bordered card grouping one cohesive set of controls. The card's outer border supplies the
 * inset and the title, so individual cards never need to re-implement spacing.
 *
 * @param title the caption naming what the card demonstrates
 * @param content the card's vertically stacked rows
 */
@Composable
internal fun Card(
    title: String,
    content: @Composable () -> Unit,
) {
    BorderPanel(
        modifier =
            SwingModifier.border(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(title),
                    BorderFactory.createEmptyBorder(ROW_GAP, ROW_GAP, ROW_GAP, ROW_GAP),
                ),
            ),
    ) {
        north {
            BoxPanel(axis = BoxLayout.Y_AXIS) {
                content()
            }
        }
    }
}

/**
 * A value-echo label of a fixed width. Echo labels in the sample track live state ("3 of 5 done"), and
 * a plain [Label] re-measures and shifts its neighbours every time that text grows or shrinks. Reserving
 * a fixed width keeps the surrounding layout perfectly still as the value changes — the single most
 * visible source of "jitter" in a reactive Swing UI.
 *
 * @param text the current value to display
 * @param width the reserved width in pixels; size it for the widest value the label will ever hold
 */
@Composable
internal fun ValueLabel(
    text: String,
    width: Int,
) {
    Label(
        text = text,
        modifier = SwingModifier.preferredWidthLabel(width),
        horizontalAlignment = SwingConstants.LEADING,
    )
}

/**
 * Pins a label to a fixed width while leaving its height to the layout. The maximum width matches the
 * preferred width so a [BoxLayout.Y_AXIS] / [BorderPanel] parent never stretches it, and the minimum width
 * holds even when the text is short — together this is what removes horizontal reflow.
 */
private fun SwingModifier.preferredWidthLabel(width: Int): SwingModifier {
    val flexibleHeight = Int.MAX_VALUE
    return this
        .preferredSize(Dimension(width, LABEL_ROW_HEIGHT))
        .minimumSize(Dimension(width, LABEL_ROW_HEIGHT))
        .maximumSize(Dimension(width, flexibleHeight))
}

private const val LABEL_ROW_HEIGHT = 22
