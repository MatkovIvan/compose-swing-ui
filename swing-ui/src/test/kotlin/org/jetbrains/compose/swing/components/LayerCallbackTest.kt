package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.layout.Panel
import org.jetbrains.compose.swing.components.layout.PanelLayout
import org.jetbrains.compose.swing.foundation.Canvas
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.layout.preferredSize
import org.jetbrains.compose.swing.modifier.listener.mouseListener
import org.jetbrains.compose.swing.test.ComposeSwingTest
import org.jetbrains.compose.swing.test.interaction.performClick
import org.jetbrains.compose.swing.test.interaction.performMouseMove
import org.jetbrains.compose.swing.test.interaction.performMouseWheel
import org.jetbrains.compose.swing.test.onWindowWithTitle
import org.jetbrains.compose.swing.test.runComposeSwingTest
import org.jetbrains.compose.swing.window.Window
import org.junit.jupiter.api.Assumptions.assumeFalse
import java.awt.AWTEvent
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Container
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.JLayer
import javax.swing.RepaintManager
import javax.swing.plaf.LayerUI
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Behavioral tests for the callback overload of [Layer]: what a paint callback shows, which events the
 * layer observes, and what a pass rewriting the callbacks costs.
 *
 * Painting is forced against an off-screen [BufferedImage] and the layer is laid out by hand first,
 * because a layer's delegate is what gives the view its bounds. The view of most tests here fills itself
 * with one color, so every pixel a callback leaves behind is known rather than compared with a golden.
 *
 * The one test that needs the layer to observe real events composes under a `Window`: a layer watches
 * events through a toolkit listener it registers only while it is displayable, and this harness's root
 * stands in no window.
 */
class LayerCallbackTest {
    private var hostRepaintManager: RepaintManager? = null

    @BeforeTest
    fun rememberRepaintManager() {
        hostRepaintManager = RepaintManager.currentManager(null)
    }

    @AfterTest
    fun restoreRepaintManager() {
        // The repaint manager is process-wide, and a recorder left installed intercepts the repaints
        // of every later test. Restoring it keeps the recording local to the test that asked for it.
        RepaintManager.setCurrentManager(hostRepaintManager)
    }

    @Test
    fun anUndeclaredPaintCallbackPaintsTheViewAsTheBareComponentPaints() = runComposeSwingTest {
        setContent {
            Panel(PanelLayout.Box()) {
                Layer(modifier = SwingModifier.testTag(LAYER), onMouseEvent = {}) {
                    Label(text = VIEW_TEXT, modifier = SwingModifier.view())
                }
                Label(text = VIEW_TEXT, modifier = SwingModifier.testTag(BARE))
            }
        }

        val decorated = render(onNodeWithTag(LAYER).fetch<JLayer<*>>())
        val bare = render(onNodeWithTag(BARE).fetch<JComponent>())

        assertTrue(
            pixelsOf(bare).any { it != 0 },
            "the bare label should paint something, or the comparison below pins nothing",
        )
        assertSamePixels(
            bare,
            decorated,
        )
    }

    @Test
    fun aPaintCallbackThatNeverPaintsTheViewLeavesItUnpainted() = runComposeSwingTest {
        setContent {
            Panel(PanelLayout.Box()) {
                Layer(modifier = SwingModifier.testTag(LAYER), onPaint = { _, _, _, _ -> }) {
                    FilledView()
                }
                Layer(modifier = SwingModifier.testTag(PLAIN), onMouseEvent = {}) {
                    FilledView()
                }
            }
        }

        val painted = pixelsOf(render(onNodeWithTag(LAYER).fetch<JLayer<*>>()))
        val plain = pixelsOf(render(onNodeWithTag(PLAIN).fetch<JLayer<*>>()))

        assertTrue(
            plain.all { alphaOf(it) == OPAQUE },
            "the same view left to paint itself should fill the layer, or the silence below pins nothing",
        )
        assertTrue(
            painted.all { it == 0 },
            "a paint callback that never calls paintView should leave the view unpainted",
        )
    }

    @Test
    fun aPaintCallbackThatPaintsTheViewUnderACompositeDimsIt() = runComposeSwingTest {
        setContent {
            Panel(PanelLayout.Box()) {
                Layer(
                    modifier = SwingModifier.testTag(LAYER),
                    onPaint = { g, _, _, paintView ->
                        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, HALF)
                        paintView()
                    },
                ) {
                    FilledView()
                }
                Layer(modifier = SwingModifier.testTag(PLAIN), onMouseEvent = {}) {
                    FilledView()
                }
            }
        }

        val dimmed = pixelsOf(render(onNodeWithTag(LAYER).fetch<JLayer<*>>()))
        val plain = pixelsOf(render(onNodeWithTag(PLAIN).fetch<JLayer<*>>()))

        assertTrue(
            plain.all { alphaOf(it) == OPAQUE },
            "the same view painted with no composite should be opaque, or the dimming below pins nothing",
        )
        assertTrue(
            dimmed.all { alphaOf(it) in 1..<OPAQUE },
            "paintView under a translucent composite should leave every pixel of the view partly " +
                "transparent",
        )
        assertTrue(
            dimmed.all { greenOf(it) > redOf(it) && greenOf(it) > blueOf(it) },
            "dimming should keep the view's own color, only less of it",
        )
    }

    @Test
    fun aPaintCallbackDrawingAfterTheViewPaintsOverIt() = runComposeSwingTest {
        setContent {
            Layer(
                modifier = SwingModifier.testTag(LAYER),
                onPaint = { g, width, height, paintView ->
                    paintView()
                    // The color is chosen after the view paints: painting the view writes the layer's own
                    // foreground into this very graphics.
                    g.color = OVERLAY
                    g.fillRect(0, 0, width, height / 2)
                },
            ) {
                FilledView()
            }
        }

        val painted = render(onNodeWithTag(LAYER).fetch<JLayer<*>>())

        assertTrue(
            rowsAre(painted, 0 until SIZE.height / 2, OVERLAY),
            "drawing after paintView should cover the view where it draws",
        )
        assertTrue(
            rowsAre(painted, SIZE.height / 2 until SIZE.height, VIEW_FILL),
            "the view should still show where the callback drew nothing over it",
        )
    }

    @Test
    fun rewritingTheCallbacksKeepsTheDelegateInstalledAndPaintsThroughTheNewestOne() = runComposeSwingTest {
        val painted = mutableListOf<Int>()
        var declaration by mutableIntStateOf(1)
        setContent {
            // Read here, in the composition, so each pass hands the layer a fresh lambda of its own.
            val captured = declaration
            Layer(
                modifier = SwingModifier.testTag(LAYER),
                onPaint = { _, _, _, paintView ->
                    painted += captured
                    paintView()
                },
            ) {
                FilledView()
            }
        }

        val layer = onNodeWithTag(LAYER).fetch<JLayer<*>>()
        val delegate: LayerUI<*> = layer.getUI()
        render(layer)
        assertEquals(listOf(1), painted, "the first paint should run the callback the first pass declared")

        declaration = 2
        awaitIdle()

        val stillInstalled: LayerUI<*> = layer.getUI()
        assertSame(
            delegate,
            stillInstalled,
            "a pass writing fresh callbacks should leave the delegate it wrote them into installed",
        )

        render(layer)
        assertEquals(
            listOf(1, 2),
            painted,
            "the paint should run the callback of the newest pass, not the one it replaced",
        )
    }

    @Test
    fun theLayerRepaintsForANewPaintCallbackAndNotForANewEventCallback() = runComposeSwingTest {
        // Each callback is held in a val of its own and captures `ran`, so a pass either hands the layer
        // the very callback it already holds or hands it a different object - never a fresh copy of the
        // same declaration.
        val ran = mutableListOf<String>()
        val firstPaint: (Graphics2D, Int, Int, () -> Unit) -> Unit = { _, _, _, paintView ->
            ran += "first paint"
            paintView()
        }
        val secondPaint: (Graphics2D, Int, Int, () -> Unit) -> Unit = { _, _, _, paintView ->
            ran += "second paint"
            paintView()
        }
        val firstMouse: (MouseEvent) -> Unit = { ran += "first mouse" }
        val secondMouse: (MouseEvent) -> Unit = { ran += "second mouse" }
        var newPaint by mutableStateOf(false)
        var newMouse by mutableStateOf(false)
        setContent {
            Layer(
                modifier = SwingModifier.testTag(LAYER).preferredSize(SIZE),
                onPaint = if (newPaint) secondPaint else firstPaint,
                onMouseEvent = if (newMouse) secondMouse else firstMouse,
            ) {
                FilledView()
            }
        }

        val layer = onNodeWithTag(LAYER).fetch<JLayer<*>>()
        var repaints = 0
        installRepaintRecorder(layer) { repaints++ }

        newMouse = true
        awaitIdle()

        assertEquals(
            0,
            repaints,
            "a pass that leaves the paint callback standing changes nothing that is on screen, so it " +
                "should ask for no repaint",
        )

        newPaint = true
        awaitIdle()

        assertTrue(
            repaints > 0,
            "a pass declaring a new paint callback should ask for the repaint that brings what it draws " +
                "to the screen. Observed $repaints repaint requests.",
        )

        render(layer)
        assertEquals(
            listOf("second paint"),
            ran,
            "the repaint should paint through the callback the newest pass declared",
        )
    }

    @Test
    fun stateReadOnlyInsideThePaintCallbackRepaintsTheLayerAndPaintsAgain() = runComposeSwingTest {
        // `veil` is NEVER read in the composition, only inside onPaint: no pass can follow a change to it,
        // and the callback the layer holds is the very one it held before, so the identity-driven repaint
        // cannot fire either. The only thing that can bring the new value to the screen is the observer
        // the paint runs under.
        val veil = mutableIntStateOf(1)
        val painted = mutableListOf<Int>()
        setContent {
            Layer(
                modifier = SwingModifier.testTag(LAYER).preferredSize(SIZE),
                onPaint = { _, _, _, paintView ->
                    painted += veil.intValue
                    paintView()
                },
            ) {
                FilledView()
            }
        }

        val layer = onNodeWithTag(LAYER).fetch<JLayer<*>>()
        // The first paint is what registers the callback's read of `veil`.
        render(layer)
        assertEquals(listOf(1), painted, "the first paint should run the callback on the value it reads")

        var repaints = 0
        installRepaintRecorder(layer) { repaints++ }
        veil.intValue = 2
        awaitIdle()

        assertTrue(
            repaints > 0,
            "a change to state the paint callback read must ask for a repaint of the layer, with no " +
                "recomposition and no paint forced by hand. Observed $repaints repaint requests.",
        )

        render(layer)
        assertEquals(
            listOf(1, 2),
            painted,
            "the repaint the change asked for should run the callback again, on the new value",
        )
    }

    @Test
    fun theEventMaskIsTheUnionOfTheDeclaredCallbacks() = runComposeSwingTest {
        setContent {
            Panel(PanelLayout.Box()) {
                Layer(modifier = SwingModifier.testTag(MOUSE_ONLY), onMouseEvent = {}) { FilledView() }
                Layer(
                    modifier = SwingModifier.testTag(MOUSE_AND_MOTION),
                    onMouseEvent = {},
                    onMouseMotionEvent = {},
                ) {
                    FilledView()
                }
                Layer(modifier = SwingModifier.testTag(WHEEL_ONLY), onMouseWheelEvent = {}) { FilledView() }
                Layer(
                    modifier = SwingModifier.testTag(PAINT_ONLY),
                    onPaint = { _, _, _, paintView -> paintView() },
                ) {
                    FilledView()
                }
            }
        }

        assertEquals(
            AWTEvent.MOUSE_EVENT_MASK,
            maskOf(MOUSE_ONLY),
            "declaring the mouse callback alone should have the layer observe mouse events alone",
        )
        assertEquals(
            AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK,
            maskOf(MOUSE_AND_MOTION),
            "declaring the motion callback beside the mouse one should add exactly the motion bit",
        )
        assertEquals(
            AWTEvent.MOUSE_WHEEL_EVENT_MASK,
            maskOf(WHEEL_ONLY),
            "declaring the wheel callback alone should have the layer observe wheel events alone",
        )
        assertEquals(
            0L,
            maskOf(PAINT_ONLY),
            "a layer that only paints should observe nothing",
        )
    }

    @Test
    fun aLayerDeclaringNoCallbackIsRefused() = runComposeSwingTest {
        val failure =
            assertFailsWith<IllegalArgumentException>("a layer that neither paints nor watches should be refused") {
                setContent {
                    Layer(modifier = SwingModifier.testTag(LAYER)) {
                        Label(text = VIEW_TEXT, modifier = SwingModifier.view())
                    }
                }
                awaitIdle()
            }

        assertTrue(
            failure.message.orEmpty().startsWith("Layer declares no callback"),
            "the refusal should name the layer, but said: ${failure.message}",
        )
    }

    @Test
    fun declaredMouseCallbacksObserveEventsThatStillReachTheView() = runComposeSwingTest {
        assumeFalse(GraphicsEnvironment.isHeadless(), "requires a display")
        val reported = mutableListOf<String>()
        setContent {
            // A layer registers its toolkit listener while it is displayable, so the layer stands under a
            // realized window; the window is never shown, since realizing its peer is all this needs.
            Window(onCloseRequest = {}, title = WINDOW_TITLE, visible = false) {
                Layer(
                    modifier = SwingModifier.testTag(LAYER),
                    onMouseEvent = { reported += "layer sees ${it.describedId}" },
                    onMouseMotionEvent = { reported += "layer sees ${it.describedId}" },
                    onMouseWheelEvent = { reported += "layer sees ${it.describedId}" },
                ) {
                    Label(
                        text = VIEW_TEXT,
                        modifier =
                            SwingModifier
                                .testTag(VIEW)
                                .view()
                                .mouseListener(onMousePressed = { reported += "view sees ${it.describedId}" }),
                    )
                }
            }
        }

        val view = onWindowWithTitle(WINDOW_TITLE).onNodeWithTag(VIEW)
        view.performClick()
        view.performMouseMove(Point(1, 1))
        view.performMouseWheel(rotation = 1)

        assertEquals(
            listOf(
                "layer sees pressed",
                "view sees pressed",
                "layer sees released",
                "layer sees clicked",
                "layer sees moved",
                "layer sees wheel",
            ),
            reported,
            "each declared callback should observe its own events, and the press should go on reaching " +
                "the view after the layer has seen it",
        )
    }
}

/**
 * A view that fills the whole layer with [VIEW_FILL], so that every pixel a paint callback leaves behind
 * is known without a golden to compare against.
 */
@Composable
private fun LayerScope.FilledView() {
    Canvas(modifier = SwingModifier.view()) { g, width, height ->
        g.color = VIEW_FILL
        g.fillRect(0, 0, width, height)
    }
}

/** The events the layer tagged [tag] observes, as the layer itself reports them. */
private fun ComposeSwingTest.maskOf(tag: String): Long = onNodeWithTag(tag).fetch<JLayer<*>>().layerEventMask

/**
 * Sizes [component] to [SIZE], lays its tree out and rasterizes it off-screen.
 *
 * The layout pass is run by hand because there is no peer to drive one, and it is what gives a layer's
 * view its bounds: a layer lays its view out through its delegate rather than through a layout manager.
 */
private fun render(component: JComponent): BufferedImage {
    component.size = SIZE
    layOutTree(component)
    val image = BufferedImage(SIZE.width, SIZE.height, TYPE)
    val graphics = image.createGraphics()
    try {
        component.paint(graphics)
    } finally {
        graphics.dispose()
    }
    return image
}

/** Runs each container's layout, top down, giving every child the bounds a paint pass reads. */
private fun layOutTree(container: Container) {
    container.doLayout()
    for (child in container.components) {
        if (child is Container) layOutTree(child)
    }
}

/**
 * Installs a [RepaintManager] that invokes [onRepaintRequest] on each repaint request targeting
 * [component]. `JComponent.repaint()` routes through `RepaintManager.addDirtyRegion`; intercepting it
 * captures the request before the manager's `isShowing()` gate would drop it off-screen. A direct
 * `paint(...)` pass, which is what [render] makes, bypasses the manager and never fires the callback.
 */
private fun installRepaintRecorder(
    component: JComponent,
    onRepaintRequest: () -> Unit,
) {
    RepaintManager.setCurrentManager(
        object : RepaintManager() {
            override fun addDirtyRegion(
                c: JComponent,
                x: Int,
                y: Int,
                w: Int,
                h: Int,
            ) {
                if (c === component) onRepaintRequest()
                super.addDirtyRegion(c, x, y, w, h)
            }
        },
    )
}

/** Every pixel of [image], row by row, as the packed ARGB values the assertions read. */
private fun pixelsOf(image: BufferedImage): IntArray =
    image.getRGB(0, 0, image.width, image.height, null, 0, image.width)

/** Whether every pixel of [image] in the rows [rows] is exactly [color]. */
private fun rowsAre(
    image: BufferedImage,
    rows: IntRange,
    color: Color,
): Boolean = rows.all { y -> (0 until image.width).all { x -> image.getRGB(x, y) == color.rgb } }

/** Fails unless [actual] holds the same pixels as [expected], reporting how many differ. */
private fun assertSamePixels(
    expected: BufferedImage,
    actual: BufferedImage,
) {
    val expectedPixels = pixelsOf(expected)
    val actualPixels = pixelsOf(actual)
    val differing = expectedPixels.indices.count { expectedPixels[it] != actualPixels[it] }
    assertEquals(
        0,
        differing,
        "a layer whose paint callback is undeclared should paint its view exactly as the view paints itself " +
            "($differing of ${expectedPixels.size} pixels differ)",
    )
}

private fun alphaOf(pixel: Int): Int = (pixel shr 24) and 0xFF

private fun redOf(pixel: Int): Int = (pixel shr 16) and 0xFF

private fun greenOf(pixel: Int): Int = (pixel shr 8) and 0xFF

private fun blueOf(pixel: Int): Int = pixel and 0xFF

/** The name this event's id reads as in a recorded order of events. */
private val MouseEvent.describedId: String
    get() =
        when (id) {
            MouseEvent.MOUSE_PRESSED -> "pressed"
            MouseEvent.MOUSE_RELEASED -> "released"
            MouseEvent.MOUSE_CLICKED -> "clicked"
            MouseEvent.MOUSE_MOVED -> "moved"
            MouseEvent.MOUSE_WHEEL -> "wheel"
            else -> "event $id"
        }

private const val LAYER = "layer-under-test"
private const val PLAIN = "undecorated-layer"
private const val BARE = "bare-label"
private const val VIEW = "layer-view"
private const val MOUSE_ONLY = "mouse-only-layer"
private const val MOUSE_AND_MOTION = "mouse-and-motion-layer"
private const val WHEEL_ONLY = "wheel-only-layer"
private const val PAINT_ONLY = "paint-only-layer"
private const val VIEW_TEXT = "decorated"
private const val WINDOW_TITLE = "layer under test"
private const val TYPE = BufferedImage.TYPE_INT_ARGB
private const val OPAQUE = 0xFF
private const val HALF = 0.5f
private val SIZE = Dimension(64, 48)
private val VIEW_FILL: Color = Color.GREEN
private val OVERLAY: Color = Color.RED
