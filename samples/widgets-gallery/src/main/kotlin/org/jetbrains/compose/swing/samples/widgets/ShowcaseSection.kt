package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.components.layout.BoxPanel
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.components.layout.ScrollPane
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.alignmentX
import org.jetbrains.compose.swing.modifier.appearance.border
import org.jetbrains.compose.swing.modifier.appearance.font
import org.jetbrains.compose.swing.modifier.appearance.foreground
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JScrollPane
import javax.swing.SwingConstants

/**
 * The left edge alignment (`0.0`) shared by every direct child of an [ExampleCard] column. A vertical
 * `BoxLayout` lines its children up by their `alignmentX`; leaf controls already report `0.0`, while
 * panels default to centered (`0.5`). Mixing the two in one column pushes the left-aligned controls
 * sideways to track the centered panels — and that offset shifts whenever the column's width changes,
 * so a control visibly jumps on toggle. Tagging every panel sibling with [LEFT_ALIGNED] keeps the whole
 * column flush-left and stable. Apply it via `SwingModifier.alignmentX(LEFT_ALIGNED)`.
 */
internal const val LEFT_ALIGNED: Float = Component.LEFT_ALIGNMENT

/**
 * A navigable section of the showcase. Each entry pairs a sidebar [title] with the composable that
 * renders the section's body, so adding a section is a single list entry — the navigation shell and
 * the body switch both read from the same source of truth.
 */
internal class ShowcaseSection(
    val title: String,
    val body: @Composable () -> Unit,
)

/** The ordered sections shown in the showcase, in sidebar order. */
internal val showcaseSections: List<ShowcaseSection> =
    listOf(
        ShowcaseSection("Components") { ComponentsSection() },
        ShowcaseSection("Form inputs") { FormInputsSection() },
        ShowcaseSection("RadioGroup") { RadioGroupSection() },
        ShowcaseSection("Rich text") { RichTextSection() },
        ShowcaseSection("Accessibility") { AccessibilitySection() },
        ShowcaseSection("Table") { TableSection() },
        ShowcaseSection("Tree") { TreeSection() },
        ShowcaseSection("Layouts") { LayoutsSection() },
        ShowcaseSection("Split & ToolBar") { SplitToolBarSection() },
        ShowcaseSection("ScrollPane") { ScrollPaneSection() },
        ShowcaseSection("Tabs") { TabsSection() },
        ShowcaseSection("Canvas") { CanvasSection() },
        ShowcaseSection("Custom component") { CustomComponentSection() },
        ShowcaseSection("Context menu") { ContextMenuSection() },
        ShowcaseSection("Data transfer") { DataTransferSection() },
        ShowcaseSection("Top-level windows") { WindowsSection() },
        ShowcaseSection("Standard dialogs") { DialogsSection() },
        ShowcaseSection("Layered & MDI") { LayeredAndMdiSection() },
        ShowcaseSection("System tray") { TraySection() },
        ShowcaseSection("Dynamic hierarchy") { DynamicHierarchySection() },
        ShowcaseSection("Composition locals") { CompositionLocalsSection() },
        ShowcaseSection("Effects") { EffectsSection() },
        ShowcaseSection("Animation") { AnimationSection() },
        ShowcaseSection("Modifier gallery") { ModifierGallery() },
    )

private const val HEADING_FONT_SIZE = 16
private const val HEADING_RGB = 0x2D4B73
private const val CAPTION_WRAP_WIDTH = 440

/**
 * A captioned card: a titled, bordered region wrapping one example. Used throughout the showcase to
 * keep each demonstrated API visually separated and labelled with the API it exercises.
 *
 * @param title the caption naming the demonstrated API
 * @param content the example composable
 */
@Composable
internal fun ExampleCard(
    title: String,
    content: @Composable () -> Unit,
) {
    BorderPanel(
        modifier =
            SwingModifier.border(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(title),
                    BorderFactory.createEmptyBorder(CARD_INSET, CARD_INSET, CARD_INSET, CARD_INSET),
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

private const val CARD_INSET = 6

/**
 * A scrollable vertical column of [ExampleCard]s, the standard body shape for a section that stacks
 * several independent examples taller than the viewport.
 *
 * The column scrolls vertically only: the horizontal scrollbar is disabled so a wide example never
 * forces a sideways scrollbar onto the whole section.
 *
 * @param cards the section's cards
 */
@Composable
internal fun SectionColumn(cards: @Composable () -> Unit) {
    ScrollPane(
        verticalScrollbar = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        horizontalScrollbar = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
    ) {
        content {
            BoxPanel(axis = BoxLayout.Y_AXIS) {
                cards()
            }
        }
    }
}

/** A bold heading row, used as the title of a section body. */
@Composable
internal fun SectionHeading(text: String) {
    FlowPanel(alignment = SwingConstants.LEADING) {
        Label(
            text = text,
            modifier =
                SwingModifier
                    .font(Font(Font.SANS_SERIF, Font.BOLD, HEADING_FONT_SIZE))
                    .foreground(Color(HEADING_RGB)),
        )
    }
}

/**
 * A short paragraph of explanatory text inside a card. The text is wrapped in an HTML body with a
 * bounded width so it flows onto multiple lines instead of forcing the layout wide — long captions
 * never trigger a horizontal scrollbar.
 */
@Composable
internal fun WrappedCaption(text: String) {
    Label(
        text = "<html><body style='width:${CAPTION_WRAP_WIDTH}px'>$text</body></html>",
        horizontalAlignment = SwingConstants.LEADING,
    )
}
