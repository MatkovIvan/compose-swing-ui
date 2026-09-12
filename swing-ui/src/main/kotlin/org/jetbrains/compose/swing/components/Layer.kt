@file:JvmMultifileClass
@file:JvmName("ComponentsKt")

package org.jetbrains.compose.swing.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import org.jetbrains.compose.swing.constants.AWTEventMask
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.listener.UNDECLARED
import org.jetbrains.compose.swing.modifier.listener.UNDECLARED_PAINT
import org.jetbrains.compose.swing.modifier.listener.declared
import org.jetbrains.compose.swing.node.SwingNode
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.JComponent
import javax.swing.JLayer
import javax.swing.plaf.LayerUI

/**
 * A decoration over one live component - a `JLayer` - that paints over what the component paints and
 * watches the mouse events that reach it, while the component goes on being itself.
 *
 * The component it decorates is declared as content, on the view region of [LayerScope]; an overlay over
 * that component goes in the glass pane beside it. The view keeps answering for itself: a widget that tells
 * a scroll pane how far to scroll it goes on telling it that through the layer, which a panel wrapping the
 * widget would not.
 *
 * ```
 * Layer(
 *     onPaint = { g, width, height, paintView ->
 *         paintView()
 *         g.paint = Color(255, 255, 255, 128)
 *         g.fillRect(0, 0, width, height)
 *     },
 * ) {
 *     Table(model = rows, modifier = SwingModifier.view())
 * }
 * ```
 *
 * Each callback is read when it fires, so writing fresh lambdas on every recomposition costs one field
 * write and installs nothing again.
 *
 * **Painting is snapshot-observed.** Snapshot state [onPaint] reads directly inside itself is tracked,
 * and a change to it repaints the layer and runs [onPaint] again - so a value the decoration draws with
 * can be read where it is drawn with, at paint time, rather than in the composition. A layer declaring
 * no paint tracks nothing.
 *
 * **The mask is derived.** A layer observes exactly the events whose callbacks this call declares:
 * declaring [onMouseEvent] alone has it observe mouse events alone, and a call that declares only
 * [onPaint] observes none. The events reach the callbacks while the layer is displayable, and a layer taken
 * off screen takes up watching again when it goes back on.
 *
 * What a layer observes it cannot swallow: a callback runs as the event is dispatched, several steps before
 * the component the event is addressed to receives it, and returning from the callback neither consumes the
 * event nor keeps it from arriving. Keeping input away from the view is a glass pane over it or a disabled
 * view, not a callback here.
 *
 * Declaring no callback at all is refused: a layer that neither paints nor watches decorates nothing.
 *
 * Reach for the overload taking a `LayerUI` where a decoration needs the rest of that type - the events
 * these callbacks do not cover, a preferred size of its own, a layout of the view of its own.
 *
 * @param modifier the [SwingModifier] applied to the underlying `JLayer`.
 * @param onPaint paints the layer, in place of the view painting itself. It receives the layer's
 *   [Graphics2D] and its current pixel `width` and `height`, and `paintView`, which paints the view and
 *   the glass pane over it: call it inside a composite to wash the view, before your own drawing to paint
 *   over it, after to paint under it, more than once, or not at all to replace it. `paintView` paints only
 *   while this call is on the stack, so run it here rather than holding on to it. The graphics is a copy
 *   the layer disposes when this returns, so a composite, transform or clip left on it reaches no other
 *   painting. Left undeclared, the layer paints the view unchanged.
 * @param onMouseEvent runs on a press, release, click, enter or exit anywhere in the layer, the view and
 *   everything under it included; declaring it has the layer observe `MOUSE_EVENT_MASK`.
 * @param onMouseMotionEvent runs as the pointer moves or drags over the layer; declaring it has the layer
 *   observe `MOUSE_MOTION_EVENT_MASK`.
 * @param onMouseWheelEvent runs as the wheel turns over the layer; declaring it has the layer observe
 *   `MOUSE_WHEEL_EVENT_MASK`.
 * @param content the composable content of the layer; see [LayerScope].
 * @see javax.swing.JLayer
 */
@Composable
public fun Layer(
    modifier: SwingModifier = SwingModifier,
    onPaint: (g: Graphics2D, width: Int, height: Int, paintView: () -> Unit) -> Unit = UNDECLARED_PAINT,
    onMouseEvent: (MouseEvent) -> Unit = UNDECLARED,
    onMouseMotionEvent: (MouseEvent) -> Unit = UNDECLARED,
    onMouseWheelEvent: (MouseWheelEvent) -> Unit = UNDECLARED,
    content: @Composable LayerScope.() -> Unit,
) {
    require(
        declared(onPaint) || declared(onMouseEvent) || declared(onMouseMotionEvent) || declared(onMouseWheelEvent),
    ) {
        "Layer declares no callback, so it paints its view unchanged and watches nothing; declare at " +
            "least one, or leave the layer out"
    }

    // One delegate per layer, never one shared between two - see CallbackLayerUI.
    val delegate = remember { CallbackLayerUI() }

    LayerNode<Component>(
        ui = delegate,
        eventMask = declaredEventMask(onMouseEvent, onMouseMotionEvent, onMouseWheelEvent),
        modifier = modifier,
        declareCallbacks = { observer ->
            // The owner's shared observer, which the delegate paints under. It is handed over before the
            // layer is attached, and stays for the layer's whole life.
            delegate.snapshotObserver = observer
            // What the layer shows is what onPaint draws, so a new one has to reach the screen: it may
            // draw with a value read in the composition, which no paint-time read tracks. The event
            // callbacks are read as their events arrive and change nothing that is on screen already.
            val paintChanged = delegate.callbacks.onPaint !== onPaint
            delegate.callbacks = LayerCallbacks(onPaint, onMouseEvent, onMouseMotionEvent, onMouseWheelEvent)
            if (paintChanged) repaint()
        },
        content = content,
    )
}

/**
 * A decoration over one live component - a `JLayer` - painted and driven by a `LayerUI` of your own,
 * which is the whole of what the JDK type offers: the events the callback overload of [Layer] does not
 * cover, a preferred size the decoration answers for, a layout of the view of its own.
 *
 * The decorated component and the overlay over it are declared as content, on the regions of
 * [LayerScope], exactly as for the callback overload.
 *
 * ```
 * Layer(ui = remember { HoverHighlightLayerUI() }, eventMask = AWTEvent.MOUSE_MOTION_EVENT_MASK) {
 *     Table(model = rows, modifier = SwingModifier.view())
 * }
 * ```
 *
 * The type parameter is the view type [ui] is written against. It fixes which delegates this call
 * accepts - a `LayerUI<JComponent>`, a `LayerUI<Component>` and a `LayerUI<JTable>` all bind without a
 * type argument at the call site - and says nothing about the child that fills the view region: no type
 * relates the two.
 *
 * @param ui the delegate the layer installs: what paints it, lays its view out, and answers for its
 *   preferred size. It is required, because a layer without a delegate paints nothing and reports a
 *   preferred size of 0x0, so the view it holds never reaches its parent's layout. It is installed by
 *   identity, so a remembered instance stays put while a fresh one on every pass is uninstalled and
 *   installed again. Give each layer a delegate of its own: installing one puts the layer on the
 *   delegate's own listener list and only installing another delegate takes it off again, so a delegate
 *   shared between two layers holds both alive and hands each of them the other's property changes.
 * @param modifier the [SwingModifier] applied to the underlying `JLayer`.
 * @param eventMask the events the layer observes. `null`, the default, leaves the mask to [ui], which is
 *   where a `LayerUI` usually sets it - from `installUI`, on the layer it was handed. A non-null value
 *   takes the mask over instead, written after the delegate is installed, so a delegate declared beside
 *   one must not set the mask itself. Withdrawing it - a non-null mask and then `null` - leaves the mask
 *   last written standing rather than handing the mask back to [ui], which only a reinstall would let set
 *   its own again; declare `0` to have the layer observe nothing. The mask reaches the layer while it is
 *   displayable, and what it observes it cannot swallow: an event reaches [ui] as it is dispatched,
 *   several steps before the component it is addressed to receives it.
 * @param content the composable content of the layer; see [LayerScope].
 * @see javax.swing.JLayer
 * @see javax.swing.JLayer.setLayerEventMask
 */
@Composable
public fun <V : Component> Layer(
    ui: LayerUI<in V>,
    modifier: SwingModifier = SwingModifier,
    @AWTEventMask eventMask: Long? = null,
    content: @Composable LayerScope.() -> Unit,
) {
    LayerNode<V>(
        ui = ui,
        eventMask = eventMask,
        modifier = modifier,
        // The delegate is the caller's; neither the callbacks nor the observer are written into it.
        declareCallbacks = {},
        content = content,
    )
}

/**
 * The node both [Layer] overloads declare.
 *
 * The delegate and the mask travel as one value because an update runs again only when its own value
 * changes: a mask declared beside a delegate that is then replaced has to be written again after the
 * replacement, and a declaration of its own would not be. The delegate is installed only when it is not
 * the one already in place, since installing a delegate uninstalls the one in place whether or not it is
 * the same object.
 *
 * `declareCallbacks` writes what the declaration holds into the delegate, on every pass, for the delegate
 * to read later. It runs on the layer, and is handed the composition owner's [SnapshotStateObserver] for
 * a delegate that paints under it; the raw overload writes neither.
 */
@Composable
private fun <V : Component> LayerNode(
    ui: LayerUI<in V>,
    eventMask: Long?,
    modifier: SwingModifier,
    declareCallbacks: JLayer<V>.(SnapshotStateObserver?) -> Unit,
    content: @Composable LayerScope.() -> Unit,
) {
    // Remembered with the layer: it holds the glass pane the layer carried before a declaration took that
    // slot, which is what an outgoing declaration puts back.
    val scope = remember { LayerScopeImpl() }

    SwingNode(
        factory = { JLayer<V>() },
        modifier = modifier,
        update = {
            set(LayerDeclaration(ui, eventMask)) { declaration ->
                if (getUI() !== declaration.ui) setUI(declaration.ui)
                if (declaration.eventMask != null) layerEventMask = declaration.eventMask
            }
            ownerObserver(declareCallbacks)
        },
        childPlacement = LayerRegions,
        content = { scope.content() },
    )
}

/** The union of the event bits whose callbacks a [Layer] declaration names. */
private fun declaredEventMask(
    onMouseEvent: (MouseEvent) -> Unit,
    onMouseMotionEvent: (MouseEvent) -> Unit,
    onMouseWheelEvent: (MouseWheelEvent) -> Unit,
): Long =
    (if (declared(onMouseEvent)) AWTEvent.MOUSE_EVENT_MASK else 0L) or
        (if (declared(onMouseMotionEvent)) AWTEvent.MOUSE_MOTION_EVENT_MASK else 0L) or
        (if (declared(onMouseWheelEvent)) AWTEvent.MOUSE_WHEEL_EVENT_MASK else 0L)

/**
 * The declaration paired by [LayerNode] so the delegate and event mask settle together.
 *
 * The node updater's `set` uses [equals] to decide whether to run an update. A [LayerUI] can override it
 * to make distinct delegates equal, but replacing a delegate is identity-sensitive because Swing owns
 * installation state on the exact instance. The mask, in contrast, is a scalar declaration and keeps
 * value equality so changing it alone still updates the layer while an equal value does not.
 */
private class LayerDeclaration<V : Component>(
    val ui: LayerUI<in V>,
    val eventMask: Long?,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is LayerDeclaration<*> && ui === other.ui && eventMask == other.eventMask)

    override fun hashCode(): Int = 31 * System.identityHashCode(ui) + eventMask.hashCode()
}

/**
 * The delegate the callback [Layer] installs: it runs the callbacks that layer declares right now where a
 * plain `LayerUI` would do its own default, and holds them in one field, so that a pass writing fresh
 * lambdas costs one write and leaves the delegate installed.
 *
 * Each layer holds one of these and never shares it: installing a delegate puts the layer on the delegate's
 * own listener list and only installing another delegate takes it off again, so a shared instance would
 * hold every layer it was installed on for as long as the process runs.
 */
private class CallbackLayerUI : LayerUI<Component>() {
    /** What the enclosing declaration holds right now, written once per pass and read as events arrive. */
    var callbacks: LayerCallbacks = UNDECLARED_CALLBACKS

    /**
     * The observer a declared paint registers its reads with, adopted from the layer's `ownerObserver`
     * in [Layer]'s update block; see the same field on `Canvas` for what the composition promises about
     * it.
     *
     * It is `null` only before the node's first update, which no paint of a composed layer can observe -
     * a declared paint fails on it.
     */
    var snapshotObserver: SnapshotStateObserver? = null

    override fun paint(
        g: Graphics,
        c: JComponent,
    ) {
        val onPaint = callbacks.onPaint
        if (onPaint === UNDECLARED_PAINT) {
            // A layer with no paint of its own reads nothing at paint time, so it observes nothing.
            paintView(g, c)
            return
        }
        val observer =
            checkNotNull(snapshotObserver) {
                "This Layer has no snapshot observer to paint under. The composition stamps the owner's " +
                    "observer onto the delegate in the node's update block, which runs before the layer " +
                    "is attached, so every layer a composition put in the Swing tree has one by its " +
                    "first paint."
            }
        // Track this paint's snapshot reads against the layer; a later change to one of them triggers
        // repaint(), which re-enters here and runs onPaint again.
        observer.observeReads(scope = c, onValueChangedForScope = { it.repaint() }) {
            onPaint(g as Graphics2D, c.width, c.height) { paintView(g, c) }
        }
    }

    override fun processMouseEvent(
        event: MouseEvent,
        layer: JLayer<out Component>,
    ) {
        callbacks.onMouseEvent(event)
    }

    override fun processMouseMotionEvent(
        event: MouseEvent,
        layer: JLayer<out Component>,
    ) {
        callbacks.onMouseMotionEvent(event)
    }

    override fun processMouseWheelEvent(
        event: MouseWheelEvent,
        layer: JLayer<out Component>,
    ) {
        callbacks.onMouseWheelEvent(event)
    }

    /** Paints the view and the glass pane over it, which is what a delegate that adds no painting does. */
    private fun paintView(
        g: Graphics,
        c: JComponent,
    ) {
        super.paint(g, c)
    }
}

/** The callbacks one [Layer] declaration holds, written into its delegate as one value. */
private class LayerCallbacks(
    val onPaint: (Graphics2D, Int, Int, () -> Unit) -> Unit,
    val onMouseEvent: (MouseEvent) -> Unit,
    val onMouseMotionEvent: (MouseEvent) -> Unit,
    val onMouseWheelEvent: (MouseWheelEvent) -> Unit,
)

/** What a delegate holds until the pass that built it writes the declaration in: nothing declared. */
private val UNDECLARED_CALLBACKS = LayerCallbacks(UNDECLARED_PAINT, UNDECLARED, UNDECLARED, UNDECLARED)
