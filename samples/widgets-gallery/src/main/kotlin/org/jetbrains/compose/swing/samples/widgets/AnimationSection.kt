@file:OptIn(ExperimentalSwingAnimationApi::class)

package org.jetbrains.compose.swing.samples.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.animation.core.ExperimentalSwingAnimationApi
import org.jetbrains.compose.swing.animation.core.FastOutSlowInEasing
import org.jetbrains.compose.swing.animation.core.RepeatMode
import org.jetbrains.compose.swing.animation.core.Spring
import org.jetbrains.compose.swing.animation.core.animateFloat
import org.jetbrains.compose.swing.animation.core.animateFloatAsState
import org.jetbrains.compose.swing.animation.core.animateIntAsState
import org.jetbrains.compose.swing.animation.core.infiniteRepeatable
import org.jetbrains.compose.swing.animation.core.rememberInfiniteTransition
import org.jetbrains.compose.swing.animation.core.spring
import org.jetbrains.compose.swing.animation.core.tween
import org.jetbrains.compose.swing.components.Canvas
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.ProgressBar
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.button.ToggleButton
import org.jetbrains.compose.swing.components.layout.FlowPanel
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.border
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.layout.alignmentX
import org.jetbrains.compose.swing.modifier.layout.maximumSize
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import java.awt.Color
import java.awt.Dimension
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import javax.swing.BorderFactory

/**
 * Demonstrates the `:swing-ui-animation` engine driving real Swing rendering over the window's frame
 * clock. Three cards cover the breadth of the API: [animateIntAsState] easing a [ProgressBar] toward a
 * preset, [animateFloatAsState] with a physical [spring] settling a hand-drawn marker, and a
 * [rememberInfiniteTransition] looping a value forever to pulse a [Canvas]. In every case the target is
 * plain hoisted Compose state and the animation interpolates toward it — no timers, no manual frames.
 */
@Composable
internal fun AnimationSection() {
    SectionColumn {
        SectionHeading("Animation")
        AnimatedProgressCard()
        SpringMarkerCard()
        InfinitePulseCard()
    }
}

/** A progress bar whose value is eased toward the chosen preset by [animateIntAsState]. */
@Composable
private fun AnimatedProgressCard() {
    ExampleCard("animateIntAsState (eased ProgressBar)") {
        var target by remember { mutableIntStateOf(0) }
        val animated by animateIntAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            label = "progress",
        )

        WrappedCaption(
            "animateIntAsState eases a Swing ProgressBar toward the chosen target across the frame clock.",
        )
        // Constrain the bar's width: a JProgressBar reports an unbounded maximum size, so in a vertical
        // BoxLayout it would stretch to the full column width and run past the visible frame. Capping the
        // maximum size keeps it inside the card.
        ProgressBar(
            value = animated,
            min = 0,
            max = FULL,
            modifier =
                SwingModifier
                    .testTag(ANIMATED_PROGRESS_TAG)
                    .maximumSize(Dimension(PROGRESS_WIDTH, PROGRESS_HEIGHT))
                    .alignmentX(LEFT_ALIGNED),
        )
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Label("Target: $target")
            Button("0%", onClick = { target = 0 })
            Button("50%", onClick = { target = MID })
            Button("100%", onClick = { target = FULL })
        }
    }
}

/**
 * A hand-drawn marker whose horizontal position is driven by a physical [spring]: toggling the target
 * sends it to the other end and it settles with bounce, demonstrating non-linear spring dynamics rather
 * than a fixed-duration tween. The animated fraction is read into the [Canvas] at paint time.
 */
@Composable
private fun SpringMarkerCard() {
    ExampleCard("animateFloatAsState (spring physics)") {
        var atEnd by remember { mutableStateOf(false) }
        val fraction by animateFloatAsState(
            targetValue = if (atEnd) 1f else 0f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "spring-marker",
        )

        WrappedCaption(
            "A spring with medium bounce settles the marker at the far end instead of stopping dead — " +
                "the position overshoots and rebounds before resting, just like a physical spring.",
        )
        Canvas(
            modifier =
                SwingModifier
                    .preferredSize(Dimension(360, 48))
                    .alignmentX(LEFT_ALIGNED)
                    .border(BorderFactory.createLineBorder(Color.GRAY)),
        ) { g, width, height ->
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = Color(0xE3, 0xF2, 0xFD)
            g.fillRect(0, 0, width, height)

            val radius = height / 2.0 - MARKER_MARGIN
            val travel = width - 2 * (radius + MARKER_MARGIN)
            val cx = radius + MARKER_MARGIN + travel * fraction
            val cy = height / 2.0
            g.color = Color(0x42, 0x85, 0xF4)
            g.fill(Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2))
        }
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            Button(if (atEnd) "Spring left" else "Spring right", onClick = { atEnd = !atEnd })
        }
    }
}

/**
 * A [rememberInfiniteTransition] loops a float between two values forever with a reversing
 * [infiniteRepeatable] spec, breathing the radius of a hand-drawn disc. The transition is composed
 * only while the toggle is on, so when it is off the section settles to a stable frame and the disc
 * holds at its resting size — the running animation is opt-in.
 */
@Composable
private fun InfinitePulseCard() {
    ExampleCard("rememberInfiniteTransition (looping pulse)") {
        var running by remember { mutableStateOf(false) }

        WrappedCaption(
            "An infinite transition reverses a value between two bounds forever, with no end state — " +
                "here it breathes the disc's radius. Toggle it off and the section rests at a stable frame.",
        )
        // Compose the transition only while running: an always-on infinite animation would never let the
        // frame clock go idle. Off, the disc paints at its resting size with no pending work.
        if (running) {
            PulsingDisc()
        } else {
            StaticDisc()
        }
        FlowPanel(modifier = SwingModifier.alignmentX(LEFT_ALIGNED)) {
            ToggleButton(text = "Running", pressed = running, onPressedChange = { running = it })
        }
    }
}

/** The disc breathing under an infinite, reversing transition; composed only while the pulse is on. */
@Composable
private fun PulsingDisc() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = PULSE_MIN,
        targetValue = PULSE_MAX,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse-scale",
    )
    Disc(scale)
}

/** The disc at its resting size, painted when the pulse is off so the section reaches a stable frame. */
@Composable
private fun StaticDisc() {
    Disc(PULSE_MAX)
}

/** A hand-drawn disc filling [scale] of the surface; the radius is read into the Canvas at paint time. */
@Composable
private fun Disc(scale: Float) {
    Canvas(
        modifier =
            SwingModifier
                .preferredSize(Dimension(120, 120))
                .alignmentX(LEFT_ALIGNED)
                .border(BorderFactory.createLineBorder(Color.GRAY)),
    ) { g, width, height ->
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val radius = minOf(width, height) / 2.0 * scale
        g.color = Color(0x42, 0x85, 0xF4)
        g.fill(Ellipse2D.Double(width / 2.0 - radius, height / 2.0 - radius, radius * 2, radius * 2))
    }
}

/** Test tag for the animated progress bar, used by the section's behavioral test. */
internal const val ANIMATED_PROGRESS_TAG: String = "animated-progress"

private const val MID = 50
private const val FULL = 100
private const val PROGRESS_WIDTH = 360
private const val PROGRESS_HEIGHT = 22
private const val MARKER_MARGIN = 4.0
private const val PULSE_MIN = 0.45f
private const val PULSE_MAX = 0.92f
