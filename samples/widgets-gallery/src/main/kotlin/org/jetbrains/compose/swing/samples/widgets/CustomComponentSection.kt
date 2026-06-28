package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.mouseListener
import org.jetbrains.compose.swing.modifier.preferredSize
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingConstants

/**
 * The extensibility lesson: the library ships built-in wrappers (Button, Slider, …), but the SAME
 * primitive that builds them — [SwingNode] — lets you fold *your own* Swing component into the composable
 * tree as a first-class citizen.
 *
 * Here [StarRating] is a hand-written `JComponent` that the library has never heard of. We wrap it with
 * one [SwingNode] call: [SwingNode.factory] constructs it, [SwingNode.update] pushes the current rating
 * onto it whenever the composable state changes, and a [mouseListener] modifier reports clicks back out.
 * From the composition's point of view it behaves exactly like a built-in widget — it sits in a panel,
 * reacts to state, and drives sibling composables (the live "N / 5" echo and the Clear button below).
 */
@Composable
internal fun CustomComponentSection() {
    SectionColumn {
        SectionHeading("Custom component")
        ExampleCard("A hand-written JComponent wrapped with SwingNode") {
            WrappedCaption(
                "StarRating is a plain Swing JComponent defined right in this sample — not a library " +
                    "wrapper. SwingNode folds it into the composition: state flows in through `update`, " +
                    "clicks flow out through a mouseListener, and it drives the composables around it.",
            )

            // The single source of truth lives in the composition, exactly as for a built-in wrapper.
            var rating by remember { mutableIntStateOf(3) }

            FlowPanel(alignment = SwingConstants.LEADING) {
                StarRating(
                    rating = rating,
                    onRatingChange = { rating = it },
                    modifier = SwingModifier.testTag(STAR_RATING_TAG),
                )
                // A sibling composable that simply reads the same state — it recomposes on every change.
                Label("$rating / $MAX_STARS")
            }

            Button(
                text = "Clear",
                onClick = { rating = 0 },
                modifier = SwingModifier.testTag(STAR_CLEAR_TAG),
            )
        }
    }
}

/**
 * Wraps the custom [StarRatingComponent] as a composable. This is all it takes to make any Swing
 * component composable: a [SwingNode] whose `factory` builds it, whose `update` maps state onto it, and
 * whose listener (attached via a [mouseListener] modifier) reports user input back to the caller.
 *
 * @param rating the number of filled stars, driven by the composition
 * @param onRatingChange invoked with the star the user clicked (1-based)
 * @param modifier the [SwingModifier] applied to the underlying component
 */
@Composable
private fun StarRating(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: SwingModifier = SwingModifier,
) {
    // rememberUpdatedState keeps the latest callback without re-attaching the listener every recomposition.
    val onChange = rememberUpdatedState(onRatingChange)
    val listener =
        remember {
            object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    val star = (event.x / (event.component.height)) + 1
                    onChange.value(star.coerceIn(1, MAX_STARS))
                }
            }
        }
    SwingNode(
        factory = { StarRatingComponent() },
        update = {
            // `set` runs on first composition and on every change of `rating`, pushing it onto the widget.
            set(rating) { this.rating = it }
            applyModifier(
                SwingModifier
                    .mouseListener(listener)
                    .preferredSize(Dimension(MAX_STARS * STAR_BOX, STAR_BOX)) then modifier,
            )
        },
    )
}

private const val MAX_STARS = 5
private const val STAR_BOX = 32

/** Test tag for the custom star-rating widget, used by the section's behavioral test. */
internal const val STAR_RATING_TAG: String = "custom-star-rating"

/** Test tag for the Clear button that resets the rating to zero. */
internal const val STAR_CLEAR_TAG: String = "custom-star-clear"

/**
 * A minimal custom Swing widget: a row of [MAX_STARS] stars, [rating] of them filled. It is an ordinary
 * `JComponent` with custom painting and a `rating` property — the library knows nothing about it; the
 * SwingNode wrapper above is what makes it composable.
 */
private class StarRatingComponent : JComponent() {
    var rating: Int = 0
        set(value) {
            val clamped = value.coerceIn(0, MAX_STARS)
            if (field != clamped) {
                field = clamped
                repaint()
            }
        }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g = graphics.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val box = height
            repeat(MAX_STARS) { index ->
                g.color = if (index < rating) FILLED else EMPTY
                g.fill(starShape(index * box, 0, box))
            }
        } finally {
            g.dispose()
        }
    }

    private fun starShape(
        x: Int,
        y: Int,
        box: Int,
    ): Polygon {
        val cx = x + box / 2.0
        val cy = y + box / 2.0
        val outer = box * OUTER_RADIUS_FRACTION
        val inner = box * INNER_RADIUS_FRACTION
        val star = Polygon()
        repeat(STAR_POINTS * 2) { step ->
            val radius = if (step % 2 == 0) outer else inner
            val angle = Math.PI / STAR_POINTS * step - Math.PI / 2
            star.addPoint(
                (cx + radius * Math.cos(angle)).toInt(),
                (cy + radius * Math.sin(angle)).toInt(),
            )
        }
        return star
    }

    private companion object {
        val FILLED = Color(0xF5, 0xA6, 0x23)
        val EMPTY = Color(0xCF, 0xCF, 0xCF)
        const val STAR_POINTS = 5
        const val OUTER_RADIUS_FRACTION = 0.45
        const val INNER_RADIUS_FRACTION = 0.18
    }
}
