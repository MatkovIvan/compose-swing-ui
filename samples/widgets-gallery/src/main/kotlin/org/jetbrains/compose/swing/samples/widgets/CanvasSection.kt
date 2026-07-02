package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Canvas
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.Slider
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.border
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.layout.alignmentX
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import javax.swing.BorderFactory

/**
 * Demonstrates [Canvas] custom drawing with the raw [Graphics2D] pipeline: a gradient-shaded
 * background, a ring of rotated translucent petals built from cubic Bézier paths, a stroked progress
 * arc, and a centred readout — none of which is a built-in widget, all of it hand-rendered.
 *
 * Both the petal count and the sweep are read *directly inside* `onDraw`: [Canvas] observes the
 * snapshot state touched while drawing, so moving either slider repaints the surface automatically —
 * no need to hoist the read into the composition or capture a value into the lambda.
 */
@Composable
internal fun CanvasSection() {
    SectionColumn {
        SectionHeading("Canvas")
        ExampleCard("Canvas (raw Graphics2D)") {
            WrappedCaption(
                "Everything here is hand-drawn through the raw Graphics2D: a diagonal gradient fill, a ring " +
                    "of rotated translucent petals built from cubic Bézier curves, and a stroked arc gauge. " +
                    "Both sliders are read at paint time, so the surface re-renders itself as you drag them.",
            )

            var petals by remember { mutableIntStateOf(8) }
            var sweep by remember { mutableIntStateOf(70) }
            FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
                Label("Petals: $petals")
                Slider(value = petals, onValueChange = { petals = it }, min = 3, max = 16)
                Label("Sweep: $sweep%")
                Slider(value = sweep, onValueChange = { sweep = it }, min = 0, max = 100)
            }

            Canvas(
                modifier =
                    SwingModifier
                        .testTag(CANVAS_TAG)
                        .preferredSize(Dimension(260, 260))
                        .alignmentX(LEFT_ALIGNED)
                        .border(BorderFactory.createLineBorder(Color.GRAY)),
            ) { g, width, height ->
                // `petals` and `sweep` are read here, at paint time. Canvas observes them, so moving
                // either slider repaints this surface automatically.
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

                paintGradientBackdrop(g, width, height)
                paintPetalRing(g, width, height, petals)
                paintArcGauge(g, width, height, sweep)
                paintCentreReadout(g, width, height, sweep)
            }
        }
    }
}

/** Fills the surface with a two-stop diagonal [GradientPaint] so the petals sit on shaded depth. */
private fun paintGradientBackdrop(
    g: Graphics2D,
    width: Int,
    height: Int,
) {
    g.paint = GradientPaint(0f, 0f, DEEP_BLUE, width.toFloat(), height.toFloat(), BRIGHT_BLUE)
    g.fillRect(0, 0, width, height)
}

/**
 * Draws [count] translucent petals around the centre. Each petal is a closed cubic Bézier path built
 * once at the top of the surface, then rotated into place with the graphics transform — the canonical
 * "one shape, many transforms" custom-drawing pattern.
 */
private fun paintPetalRing(
    g: Graphics2D,
    width: Int,
    height: Int,
    count: Int,
) {
    val centreX = width / 2.0
    val centreY = height / 2.0
    val reach = minOf(width, height) / 2.0 * PETAL_REACH_FRACTION

    // Control points as fractions of the petal's reach: a narrow waist near the centre that fans out
    // to a rounded tip, mirrored across the vertical axis to close the leaf shape.
    val waist = reach * PETAL_WAIST_FRACTION
    val shoulder = reach * PETAL_SHOULDER_FRACTION
    val petal =
        Path2D.Double().apply {
            moveTo(0.0, 0.0)
            curveTo(waist, -reach * PETAL_NECK_FRACTION, waist, -shoulder, 0.0, -reach)
            curveTo(-waist, -shoulder, -waist, -reach * PETAL_NECK_FRACTION, 0.0, 0.0)
            closePath()
        }

    val fill = whiteAlpha(PETAL_ALPHA)
    val stroke = whiteAlpha(PETAL_STROKE_ALPHA)
    g.stroke = BasicStroke(PETAL_STROKE_WIDTH)
    repeat(count) { index ->
        val saved = g.transform
        g.translate(centreX, centreY)
        g.rotate(FULL_TURN_RADIANS * index / count)
        g.color = fill
        g.fill(petal)
        g.color = stroke
        g.draw(petal)
        g.transform = saved
    }
}

/** A translucent white at the given [alpha], reused for the petals' fill and stroke. */
private fun whiteAlpha(alpha: Int): Color = Color(WHITE_RGB, WHITE_RGB, WHITE_RGB, alpha)

/**
 * Draws a stroked arc that fills clockwise from the top in proportion to [percent], with a faint full
 * track behind it — a hand-built progress gauge using [Arc2D] and a round-capped [BasicStroke].
 */
private fun paintArcGauge(
    g: Graphics2D,
    width: Int,
    height: Int,
    percent: Int,
) {
    val inset = minOf(width, height) * GAUGE_INSET_FRACTION
    val size = minOf(width, height) - inset - inset
    val x = (width - size) / 2.0
    val y = (height - size) / 2.0

    g.stroke = BasicStroke(GAUGE_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g.color = whiteAlpha(GAUGE_TRACK_ALPHA)
    g.draw(Arc2D.Double(x, y, size, size, 0.0, FULL_CIRCLE, Arc2D.OPEN))

    val extent = -FULL_CIRCLE * percent / PERCENT_MAX
    g.color = AMBER
    g.draw(Arc2D.Double(x, y, size, size, GAUGE_START_ANGLE, extent, Arc2D.OPEN))
}

/** Paints the percentage as antialiased text inside a small disc at the centre of the gauge. */
private fun paintCentreReadout(
    g: Graphics2D,
    width: Int,
    height: Int,
    percent: Int,
) {
    val discSize = minOf(width, height) * DISC_FRACTION
    val discX = (width - discSize) / 2.0
    val discY = (height - discSize) / 2.0
    g.color = DEEP_BLUE
    g.fill(Ellipse2D.Double(discX, discY, discSize, discSize))

    val text = "$percent%"
    g.font = Font(Font.SANS_SERIF, Font.BOLD, READOUT_FONT_SIZE)
    g.color = Color.WHITE
    val metrics = g.fontMetrics
    val textX = (width - metrics.stringWidth(text)) / 2
    val textY = height / 2 + metrics.ascent / 2 - metrics.descent / 2
    g.drawString(text, textX, textY)
}

/** Test tag for the [CanvasSection] drawing surface, used by its behavioral tests to locate it. */
internal const val CANVAS_TAG = "canvas-section-surface"

// Palette, named once because the painting helpers below are plain functions, not @Composable.
private val DEEP_BLUE = Color(0x0D, 0x47, 0xA1)
private val BRIGHT_BLUE = Color(0x42, 0x85, 0xF4)
private val AMBER = Color(0xFF, 0xC1, 0x07)
private const val WHITE_RGB = 255

private const val PETAL_REACH_FRACTION = 0.92
private const val PETAL_WAIST_FRACTION = 0.45
private const val PETAL_NECK_FRACTION = 0.30
private const val PETAL_SHOULDER_FRACTION = 0.85
private const val PETAL_ALPHA = 60
private const val PETAL_STROKE_ALPHA = 120
private const val PETAL_STROKE_WIDTH = 1.5f
private val FULL_TURN_RADIANS = 2 * Math.PI
private const val GAUGE_INSET_FRACTION = 0.12
private const val GAUGE_STROKE = 8f
private const val GAUGE_TRACK_ALPHA = 70
private const val GAUGE_START_ANGLE = 90.0
private const val FULL_CIRCLE = 360.0
private const val PERCENT_MAX = 100
private const val DISC_FRACTION = 0.34
private const val READOUT_FONT_SIZE = 22
