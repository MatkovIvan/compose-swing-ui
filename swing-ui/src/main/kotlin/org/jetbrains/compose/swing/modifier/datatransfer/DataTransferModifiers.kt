package org.jetbrains.compose.swing.modifier.datatransfer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.swing.constants.TransferAction
import org.jetbrains.compose.swing.modifier.SwingModifier
import java.awt.Point
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DragSource
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.TransferHandler
import kotlin.math.abs

/*
 * Data-transfer SwingModifiers — drag-and-drop and system-clipboard support. Drag source, drop
 * target, and clipboard copy/cut/paste can all be declared on the same component and coexist. The set
 * of allowed operations is a [TransferAction] `TransferHandler` action bit-mask.
 */

/**
 * Makes the component a drag SOURCE that exports a [Transferable] when dragged.
 *
 * [exportedActions] declares which operations the source offers (a [TransferAction] `TransferHandler`
 * action bit-mask, e.g. `TransferHandler.COPY`, `TransferHandler.COPY_OR_MOVE`); a drop is permitted
 * only for an operation also accepted by the target. [transferable] is invoked when a drag begins on
 * the component (already typed [JComponent]) and returns the data to transfer, or `null` to start no
 * drag.
 *
 * Requires a [JComponent] target. Composes with [dropTarget] and [clipboard] on the same component:
 * all three configure one underlying transfer handler, so a component can be a drag source, a drop
 * target, and clipboard-enabled at once. [transferable] and [exportedActions] are read on each drag,
 * so passing fresh values across recompositions takes effect immediately.
 */
public fun SwingModifier.draggable(
    @TransferAction exportedActions: Int,
    transferable: (component: JComponent) -> Transferable?,
): SwingModifier = this then DraggableElement(exportedActions, transferable)

/**
 * Makes the component a drop TARGET that imports a dropped [Transferable].
 *
 * [acceptedActions] declares which operations the target accepts; a drop whose operation is not among
 * them is rejected before [onDrop]. [canImport] gates a drop by its offered [DataFlavor]s — it
 * receives the dragged data's flavors and returns whether the drop may proceed; the default accepts
 * any flavor, so gating is opt-in. [onDrop] is invoked on an accepted drop with the dropped
 * [Transferable] and returns whether the import succeeded.
 *
 * Requires a [JComponent] target. Composes with [draggable] and [clipboard] on the same component.
 * [onDrop], [canImport], and [acceptedActions] are read on each drop, so passing fresh values across
 * recompositions takes effect immediately.
 */
public fun SwingModifier.dropTarget(
    @TransferAction acceptedActions: Int,
    onDrop: (transferable: Transferable) -> Boolean,
    canImport: (flavors: List<DataFlavor>) -> Boolean = { true },
): SwingModifier = this then DropTargetElement(acceptedActions, onDrop, canImport)

/**
 * Enables system-clipboard copy/cut export and paste import on the component, over the same transfer
 * handler [draggable] and [dropTarget] use.
 *
 * Copy and cut export the value [transferable] produces to the system clipboard; the difference is the
 * operation reported to [transferable]'s caller via the standard export contract
 * (`TransferHandler.COPY` versus `TransferHandler.MOVE`). Paste reads the system clipboard and, when
 * [canImport] accepts the
 * available flavors, hands the clipboard [Transferable] to [onPaste], which returns whether the import
 * succeeded. [canImport] defaults to accepting any flavor.
 *
 * When [bindKeys] is `true` (the default), the platform copy/cut/paste keystrokes (the menu-shortcut
 * modifier with `C`/`X`/`V`) are bound on the component so the standard shortcuts trigger these
 * operations while it is focused; pass `false` to enable the operations without installing key
 * bindings (e.g. to drive them from your own menu). Requires a [JComponent] target. All callbacks are
 * read live, so passing fresh lambdas across recompositions takes effect immediately.
 *
 * Pass a [handle] from [rememberClipboardHandle] to also trigger copy/cut/paste programmatically. The
 * handle binds to the component this modifier is applied to, so [ClipboardHandle.copy],
 * [ClipboardHandle.cut] and [ClipboardHandle.paste] drive these same operations from an event handler
 * (e.g. a menu item) without the caller ever touching the [JComponent].
 */
public fun SwingModifier.clipboard(
    transferable: (component: JComponent) -> Transferable?,
    onPaste: (transferable: Transferable) -> Boolean,
    canImport: (flavors: List<DataFlavor>) -> Boolean = { true },
    bindKeys: Boolean = true,
    handle: ClipboardHandle? = null,
): SwingModifier = this then ClipboardElement(transferable, onPaste, canImport, bindKeys, handle)

/**
 * A programmatic trigger for the clipboard copy/cut/paste of the component a [clipboard] modifier binds
 * it to. Obtain one from [rememberClipboardHandle], pass it to `clipboard(handle = …)`, then call
 * [copy]/[cut]/[paste] from an event handler (e.g. a menu item) to drive the same operations the bound
 * keystrokes do — without ever holding the underlying [JComponent].
 *
 * The handle captures the component (and its transfer handler) the modifier binds it to; while unbound —
 * before the modifier applies or after it leaves the chain — [copy] and [cut] are no-ops and [paste]
 * returns `false`.
 */
public class ClipboardHandle internal constructor() {
    // The component the clipboard modifier bound this handle to, or null while unbound. Copy/cut/paste
    // operate on it through the transfer handler that same modifier installed.
    private var component: JComponent? = null

    /**
     * Copies to the system clipboard: exports the value the bound component's [clipboard] modifier
     * produces as a `TransferHandler.COPY`. A no-op while the handle is unbound.
     */
    public fun copy() {
        component?.let { exportToClipboard(it, TransferHandler.COPY) }
    }

    /**
     * Cuts to the system clipboard: exports the value the bound component's [clipboard] modifier
     * produces as a `TransferHandler.MOVE`. A no-op while the handle is unbound.
     */
    public fun cut() {
        component?.let { exportToClipboard(it, TransferHandler.MOVE) }
    }

    /**
     * Pastes from the system clipboard: reads the clipboard and, when the bound component's [clipboard]
     * modifier's `canImport` accepts the flavors, imports the contents through that modifier's
     * `onPaste`. Returns whether the import succeeded; returns `false` while the handle is unbound or
     * the clipboard is empty.
     */
    public fun paste(): Boolean {
        val component = component ?: return false
        val handler = component.transferHandler
        val contents = clipboardContents()
        return handler != null &&
            contents != null &&
            handler.importData(TransferHandler.TransferSupport(component, contents))
    }

    /** Binds the handle to [target] so copy/cut/paste act on it; called by the [clipboard] modifier. */
    internal fun bind(target: JComponent) {
        component = target
    }

    /** Unbinds [target] if it is the currently bound component, leaving a handle bound elsewhere intact. */
    internal fun unbind(target: JComponent) {
        if (component === target) component = null
    }
}

/**
 * Creates and remembers a [ClipboardHandle] to drive a component's clipboard copy/cut/paste
 * programmatically. Pass it to `clipboard(handle = …)` to bind it to that component, then call
 * [ClipboardHandle.copy]/[ClipboardHandle.cut]/[ClipboardHandle.paste] from an event handler.
 */
@Composable
public fun rememberClipboardHandle(): ClipboardHandle = remember { ClipboardHandle() }

private class DraggableElement(
    @param:TransferAction private val exportedActions: Int,
    private val transferable: (JComponent) -> Transferable?,
) : SwingModifier.Element<JComponent, DraggableElement.Node> {
    override val targetType: Class<JComponent> get() = JComponent::class.java

    override fun create(): Node = Node()

    override fun update(node: Node) {
        node.config = SourceConfig(exportedActions, transferable)
        node.apply()
    }

    class Node : SwingModifier.Node<JComponent>() {
        var config: SourceConfig? = null
        private var sourceToken: SliceToken? = null
        private var gesture: DragGesture? = null

        override fun onAttach() {
            // A generic component has no built-in drag gesture, so installing only the TransferHandler
            // would never start a drag. Install a mouse gesture that, past the platform threshold, asks
            // the handler to export the drag — once per node, removed on detach.
            val gesture = DragGesture(component)
            component.addMouseListener(gesture)
            component.addMouseMotionListener(gesture)
            this.gesture = gesture
        }

        fun apply() {
            val config = config ?: return
            val handler = installedHandler(component)
            // Claim the slice once under a stable token, then refresh the config under it: the config is
            // a fresh value each recomposition, so it is not a stable ownership key — the token is.
            if (!handler.setSource(sourceToken, config)) {
                sourceToken = handler.installSource(config)
            }
        }

        override fun onDetach() {
            gesture?.let {
                component.removeMouseListener(it)
                component.removeMouseMotionListener(it)
            }
            gesture = null
            clearSourceIfOwned(component, sourceToken)
            sourceToken = null
            uninstallIfEmpty(component)
        }
    }
}

/**
 * The drag gesture a [draggable] component installs: tracks the press point and, once the pointer
 * moves past the platform drag threshold, hands the drag to the component's [TransferHandler] via
 * [TransferHandler.exportAsDrag]. The handler then pulls the [Transferable] from its source slice, so
 * the same exporter drives both drag and clipboard while only a real drag fires this path.
 */
private class DragGesture(
    private val component: JComponent,
) : MouseAdapter() {
    private var origin: Point? = null

    override fun mousePressed(event: MouseEvent) {
        origin = event.point
    }

    override fun mouseReleased(event: MouseEvent) {
        origin = null
    }

    override fun mouseDragged(event: MouseEvent) {
        val start = origin ?: return
        val threshold = DragSource.getDragThreshold()
        if (abs(event.x - start.x) < threshold && abs(event.y - start.y) < threshold) return
        origin = null
        // exportAsDrag verifies the requested action against getSourceActions before starting; pass
        // the source's own offered action so it matches what the handler reports.
        component.transferHandler?.let { handler ->
            handler.exportAsDrag(component, event, handler.getSourceActions(component))
        }
    }
}

private class DropTargetElement(
    @param:TransferAction private val acceptedActions: Int,
    private val onDrop: (Transferable) -> Boolean,
    private val canImport: (List<DataFlavor>) -> Boolean,
) : SwingModifier.Element<JComponent, DropTargetElement.Node> {
    override val targetType: Class<JComponent> get() = JComponent::class.java

    override fun create(): Node = Node()

    override fun update(node: Node) {
        node.config = DropConfig(acceptedActions, onDrop, canImport)
        node.apply()
    }

    class Node : SwingModifier.Node<JComponent>() {
        var config: DropConfig? = null
        private var dropToken: SliceToken? = null

        fun apply() {
            val config = config ?: return
            val handler = installedHandler(component)
            // Claim the slice once under a stable token, then refresh the config under it.
            if (!handler.setDrop(dropToken, config)) {
                dropToken = handler.installDrop(config)
            }
        }

        override fun onDetach() {
            clearDropIfOwned(component, dropToken)
            dropToken = null
            uninstallIfEmpty(component)
        }
    }
}

private class ClipboardElement(
    private val transferable: (JComponent) -> Transferable?,
    private val onPaste: (Transferable) -> Boolean,
    private val canImport: (List<DataFlavor>) -> Boolean,
    private val bindKeys: Boolean,
    private val handle: ClipboardHandle?,
) : SwingModifier.Element<JComponent, ClipboardElement.Node> {
    override val targetType: Class<JComponent> get() = JComponent::class.java

    override fun create(): Node = Node(bindKeys)

    override fun update(node: Node) {
        node.transferable = transferable
        node.onPaste = onPaste
        node.canImport = canImport
        node.handle = handle
        node.apply()
    }

    class Node(
        private val bindKeys: Boolean,
    ) : SwingModifier.Node<JComponent>() {
        var transferable: (JComponent) -> Transferable? = { null }
        var onPaste: (Transferable) -> Boolean = { false }
        var canImport: (List<DataFlavor>) -> Boolean = { true }

        // The handle to drive copy/cut/paste, read live so a handle passed on a later recomposition
        // binds to this component and one that leaves (or is replaced) unbinds. The field holds the
        // currently bound handle, so onDetach unbinds exactly the one this node bound.
        var handle: ClipboardHandle? = null
            set(value) {
                if (value === field) return
                field?.unbind(component)
                field = value
                value?.bind(component)
            }

        private var sourceToken: SliceToken? = null
        private var dropToken: SliceToken? = null
        private var keysBound = false

        fun apply() {
            val component = component
            val handler = installedHandler(component)
            // Clipboard export reuses the drag source's exporter (copy/cut both produce the same
            // value); declaring COPY_OR_MOVE lets exportToClipboard run for either action. Only own the
            // source slice when no drag source already holds it, so a sibling draggable's export is left
            // intact.
            val sourceConfig = SourceConfig(TransferHandler.COPY_OR_MOVE) { transferable(it) }
            // Refresh under our token if we still own it; otherwise claim it only while it is free, so a
            // sibling draggable's export is left intact.
            if (!handler.setSource(sourceToken, sourceConfig) && handler.source == null) {
                sourceToken = handler.installSource(sourceConfig)
            }
            val dropConfig =
                DropConfig(TransferHandler.COPY_OR_MOVE, { onPaste(it) }, { canImport(it) })
            if (!handler.setDrop(dropToken, dropConfig)) {
                dropToken = handler.installDrop(dropConfig)
            }
            if (bindKeys && !keysBound) {
                bindClipboardKeys(component)
                keysBound = true
            }
        }

        override fun onDetach() {
            val component = component
            // Clearing the handle runs its setter, which unbinds this component from the bound handle.
            handle = null
            clearSourceIfOwned(component, sourceToken)
            clearDropIfOwned(component, dropToken)
            sourceToken = null
            dropToken = null
            if (keysBound) unbindClipboardKeys(component)
            keysBound = false
            uninstallIfEmpty(component)
        }
    }
}

private const val COPY_KEY = "swing-ui.clipboard.copy"
private const val CUT_KEY = "swing-ui.clipboard.cut"
private const val PASTE_KEY = "swing-ui.clipboard.paste"

private fun bindClipboardKeys(component: JComponent) {
    val shortcut = menuShortcutMask()
    val inputMap = component.getInputMap(JComponent.WHEN_FOCUSED)
    inputMap.put(KeyStroke.getKeyStroke('C'.code, shortcut), COPY_KEY)
    inputMap.put(KeyStroke.getKeyStroke('X'.code, shortcut), CUT_KEY)
    inputMap.put(KeyStroke.getKeyStroke('V'.code, shortcut), PASTE_KEY)
    val actionMap = component.actionMap
    actionMap.put(COPY_KEY, TransferHandler.getCopyAction())
    actionMap.put(CUT_KEY, TransferHandler.getCutAction())
    actionMap.put(PASTE_KEY, TransferHandler.getPasteAction())
}

private fun unbindClipboardKeys(component: JComponent) {
    val shortcut = menuShortcutMask()
    val inputMap = component.getInputMap(JComponent.WHEN_FOCUSED)
    inputMap.remove(KeyStroke.getKeyStroke('C'.code, shortcut))
    inputMap.remove(KeyStroke.getKeyStroke('X'.code, shortcut))
    inputMap.remove(KeyStroke.getKeyStroke('V'.code, shortcut))
    val actionMap = component.actionMap
    actionMap.remove(COPY_KEY)
    actionMap.remove(CUT_KEY)
    actionMap.remove(PASTE_KEY)
}

/** A drag-source/clipboard-export slice: what to export and which operations it offers. */
internal class SourceConfig(
    val exportedActions: Int,
    val transferable: (JComponent) -> Transferable?,
)

/** A drop-target/clipboard-import slice: which operations and flavors it accepts and how to import. */
internal class DropConfig(
    val acceptedActions: Int,
    val onImport: (Transferable) -> Boolean,
    val canImport: (List<DataFlavor>) -> Boolean,
)

/**
 * The one `TransferHandler` every data-transfer modifier on a component configures. It holds a
 * nullable [source] (drag/clipboard export) and [drop] (drop/clipboard import) slice and routes the
 * `TransferHandler` callbacks to whichever slices are present, so drag, drop, and clipboard coexist on
 * a single component. [original] is the handler the component had before this one was installed, so it
 * can be restored once every slice is gone.
 *
 * Each slice is owned by exactly one node. Ownership is tracked by an opaque [SliceToken] issued when a
 * node installs the slice (via [installSource]/[installDrop]) and remembered by that node, rather than
 * by the identity of the [SourceConfig]/[DropConfig] value itself: a node refreshes its config on every
 * recomposition (a fresh value each time), so config identity is not a stable ownership key, whereas
 * the token is minted once per install and stays put. A node clears its slice through
 * [clearSourceIfOwned]/[clearDropIfOwned], which release the slice only when the live token still
 * matches the one the node holds — so a slice another node has since taken over is never cleared.
 */
internal class SharedTransferHandler : TransferHandler() {
    var original: TransferHandler? = null

    var source: SourceConfig? = null
        private set
    var drop: DropConfig? = null
        private set

    private var sourceToken: SliceToken? = null
    private var dropToken: SliceToken? = null

    /**
     * Sets [config] as the source slice and takes ownership under a freshly minted [SliceToken], which
     * is returned. Use this to claim the slice; refresh the config under the same token with
     * [setSource].
     */
    fun installSource(config: SourceConfig): SliceToken {
        source = config
        return SliceToken().also { sourceToken = it }
    }

    /**
     * Sets [config] as the drop slice and takes ownership under a freshly minted [SliceToken], which is
     * returned. Use this to claim the slice; refresh the config under the same token with [setDrop].
     */
    fun installDrop(config: DropConfig): SliceToken {
        drop = config
        return SliceToken().also { dropToken = it }
    }

    /**
     * Refreshes the source config under the existing owning [token] without minting a new one. Returns
     * whether [token] still owns the slice (and the config was applied); a caller that gets `false` must
     * re-[installSource] to reclaim it.
     */
    fun setSource(
        token: SliceToken?,
        config: SourceConfig,
    ): Boolean {
        if (token == null || sourceToken !== token) return false
        source = config
        return true
    }

    /**
     * Refreshes the drop config under the existing owning [token] without minting a new one. Returns
     * whether [token] still owns the slice (and the config was applied); a caller that gets `false` must
     * re-[installDrop] to reclaim it.
     */
    fun setDrop(
        token: SliceToken?,
        config: DropConfig,
    ): Boolean {
        if (token == null || dropToken !== token) return false
        drop = config
        return true
    }

    /** Clears the source slice only if [token] still owns it; returns whether it was cleared. */
    fun clearSource(token: SliceToken): Boolean {
        if (sourceToken !== token) return false
        source = null
        sourceToken = null
        return true
    }

    /** Clears the drop slice only if [token] still owns it; returns whether it was cleared. */
    fun clearDrop(token: SliceToken): Boolean {
        if (dropToken !== token) return false
        drop = null
        dropToken = null
        return true
    }

    override fun getSourceActions(c: JComponent?): Int = source?.exportedActions ?: NONE

    override fun createTransferable(c: JComponent): Transferable? = source?.transferable?.invoke(c)

    override fun canImport(support: TransferSupport): Boolean {
        val config = drop ?: return false
        val actionAccepted = !support.isDrop || (config.acceptedActions and support.dropAction) != 0
        return actionAccepted && config.canImport(support.dataFlavors.asList())
    }

    override fun importData(support: TransferSupport): Boolean {
        val config = drop ?: return false
        return canImport(support) && config.onImport(support.transferable)
    }
}

/**
 * An opaque per-install capability token for one slice of a [SharedTransferHandler]. Minted by
 * [SharedTransferHandler.installSource]/[SharedTransferHandler.installDrop] and held by the installing
 * node so it — and only it — can later release the slice it installed.
 */
internal class SliceToken
