@file:JvmMultifileClass
@file:JvmName("LayoutComponentsKt")

package org.jetbrains.compose.swing.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SlotNode
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.components.SyntheticIds
import org.jetbrains.compose.swing.constants.TabLayoutPolicy
import org.jetbrains.compose.swing.constants.TabPlacement
import org.jetbrains.compose.swing.core.SlotAttachment
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.changeListener
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.event.ChangeListener

/**
 * A composable wrapper for `JTabbedPane` with declarative, dynamic tabs.
 *
 * Declare tabs in [block]; each `tab(...)` becomes a titled tab hosting its body composable. Tabs are
 * **dynamic**: adding or removing a `tab(...)` in the composition adds or removes the matching tab, and
 * a tab's title/icon/tooltip/enabled update on recomposition. The selected tab is controlled via
 * [selectedIndex].
 *
 * ```
 * TabbedPane(selectedIndex = sel, onSelectedIndexChange = { sel = it }) {
 *     tab("General") { GeneralPanel() }
 *     tab("Advanced", enabled = false) { AdvancedPanel() }
 * }
 * ```
 *
 * @param selectedIndex the index of the selected tab (controlled)
 * @param modifier the [SwingModifier] applied to the underlying `JTabbedPane`
 * @param onSelectedIndexChange callback invoked when the selected tab changes
 * @param tabPlacement where the tab strip is drawn
 * @param tabLayoutPolicy how the tab strip handles overflow
 * @param block declares the tabs; see [TabbedPaneScope]
 */
@Composable
public fun TabbedPane(
    selectedIndex: Int,
    modifier: SwingModifier = SwingModifier,
    onSelectedIndexChange: (Int) -> Unit = {},
    @TabPlacement tabPlacement: Int = JTabbedPane.TOP,
    @TabLayoutPolicy tabLayoutPolicy: Int = JTabbedPane.WRAP_TAB_LAYOUT,
    block: TabbedPaneScope.() -> Unit,
) {
    val callback = rememberUpdatedState(onSelectedIndexChange)
    val listener = remember { ChangeListener { event -> callback.value((event.source as JTabbedPane).selectedIndex) } }
    TabbedPane(
        selectedIndex = selectedIndex,
        changeListener = listener,
        modifier = modifier,
        tabPlacement = tabPlacement,
        tabLayoutPolicy = tabLayoutPolicy,
        block = block,
    )
}

/**
 * A [TabbedPane] driven by a raw [ChangeListener] instead of an `onSelectedIndexChange` lambda. The
 * listener is attached as-is and removed on the same instance; pass a stable instance (e.g.
 * `remember {}`) to avoid churn.
 *
 * @param selectedIndex the index of the selected tab (controlled)
 * @param changeListener the listener notified when the selected tab changes
 * @param modifier the [SwingModifier] applied to the underlying `JTabbedPane`
 * @param tabPlacement where the tab strip is drawn
 * @param tabLayoutPolicy how the tab strip handles overflow
 * @param block declares the tabs; see [TabbedPaneScope]
 */
@Composable
public fun TabbedPane(
    selectedIndex: Int,
    changeListener: ChangeListener,
    modifier: SwingModifier = SwingModifier,
    @TabPlacement tabPlacement: Int = JTabbedPane.TOP,
    @TabLayoutPolicy tabLayoutPolicy: Int = JTabbedPane.WRAP_TAB_LAYOUT,
    block: TabbedPaneScope.() -> Unit,
) {
    TabbedPaneImpl(
        selectedIndex = selectedIndex,
        modifier = modifier,
        tabPlacement = tabPlacement,
        tabLayoutPolicy = tabLayoutPolicy,
        selectionListener = { pane -> pane.changeListener(changeListener) },
        block = block,
    )
}

@Composable
private fun TabbedPaneImpl(
    selectedIndex: Int,
    modifier: SwingModifier,
    @TabPlacement tabPlacement: Int,
    @TabLayoutPolicy tabLayoutPolicy: Int,
    selectionListener: (SwingModifier) -> SwingModifier,
    block: TabbedPaneScope.() -> Unit,
) {
    // Collect tab declarations fresh on every composition so a tab the caller stops declaring (e.g.
    // behind an `if`) drops out of `content`, and the applier uninstalls its page via the slot
    // mechanism. A remembered, mutated scope would retain the stale declaration.
    val scope = TabbedPaneScopeImpl().apply(block)
    val pane = remember { JTabbedPane() }

    SwingNode(
        factory = { pane },
        update = {
            set(tabPlacement) { this.tabPlacement = it }
            set(tabLayoutPolicy) { this.tabLayoutPolicy = it }
            // Apply selection through reconcile, not set(selectedIndex): the tabs are emitted in
            // `content`, which the runtime applies AFTER this update block, so on the composition that
            // first adds a tab the page does not exist yet when set() would run. reconcile runs after
            // every change pass and re-checks tabCount, so the controlled index lands once its tab is
            // present and is re-asserted if the strip changes underneath it.
            reconcile {
                if (selectedIndex in 0 until this.tabCount && this.selectedIndex != selectedIndex) {
                    this.selectedIndex = selectedIndex
                }
            }
            applyModifier(selectionListener(SwingModifier) then modifier)
        },
        content = {
            scope.tabs.forEach { tab ->
                // key() gives each tab a composition identity by its synthetic id rather than its list
                // position, so the tab keeps its slot — and the state inside it — even if the
                // surrounding declarations shift around it.
                key(tab.id) {
                    // The attachment captures only the install-time title/icon/tooltip; later edits
                    // flow through the body node's update block below, so the remembered attachment
                    // never needs to see fresh metadata.
                    val attachment =
                        remember(pane) {
                            tabAttachment(tab.title, tab.icon, tab.tooltip, tab.enabled)
                        }
                    SlotNode(attachment) {
                        // The body host IS the tab's component. Reading pane.indexOfComponent(this)
                        // gives its live position, so metadata writes stay correct even after earlier
                        // tabs are added or removed. Routing metadata through this update block keeps
                        // it driven by recomposition rather than a side effect.
                        //
                        // On the FIRST update the slot is not yet attached (the applier inserts the
                        // component after these fixups run), so indexOfComponent is -1 and the writes
                        // skip — the attachment applied the initial metadata at insertTab time. Every
                        // later recomposition runs with the tab attached, so changed values land.
                        SwingNode(
                            factory = { JPanel(BorderLayout()) },
                            update = {
                                // Resolve the live tab position once per update pass and reuse it
                                // across all four metadata writes. The host panel (`this` inside these
                                // blocks) is the tab's component; its index in `pane` is the same for
                                // every field, so reading it once avoids four redundant lookups.
                                // `reconcile` runs first on every pass and exposes the component as
                                // `this`, so `at` is captured before the set blocks below read it.
                                //
                                // The >= 0 guard stays INSIDE each set block (not hoisted around the set
                                // calls): set() must visit its slot on every pass to stay positionally
                                // aligned, so the calls themselves remain unconditional and only the
                                // write is guarded. On the FIRST update the slot is not yet attached, so
                                // `at` is -1 and the writes skip — the attachment already applied the
                                // initial metadata at insertTab time.
                                var at = -1
                                reconcile { at = pane.indexOfComponent(this) }
                                set(tab.title) { if (at >= 0) pane.setTitleAt(at, it) }
                                set(tab.icon) { if (at >= 0) pane.setIconAt(at, it) }
                                set(tab.tooltip) { if (at >= 0) pane.setToolTipTextAt(at, it) }
                                set(tab.enabled) { if (at >= 0) pane.setEnabledAt(at, it) }
                            },
                            content = { tab.content() },
                        )
                    }
                }
            }
        },
    )
}

/**
 * One declared tab: a synthetic [id] stable across recompositions, its metadata snapshot for this
 * composition, plus its body composable.
 */
private class TabDeclaration(
    val id: Int,
    val title: String,
    val icon: Icon?,
    val tooltip: String?,
    val enabled: Boolean,
    val content: @Composable () -> Unit,
)

private class TabbedPaneScopeImpl : TabbedPaneScope {
    val tabs: MutableList<TabDeclaration> = ArrayList()

    private val ids = SyntheticIds()

    override fun tab(
        title: String,
        icon: Icon?,
        tooltip: String?,
        enabled: Boolean,
        content: @Composable () -> Unit,
    ) {
        tabs.add(TabDeclaration(ids.next(), title, icon, tooltip, enabled, content))
    }
}

/**
 * Builds a [SlotAttachment] that hosts one tab's body panel in [pane] via `insertTab`.
 *
 * Install creates the tab at the applier-supplied composition index with its initial title/icon/
 * tooltip/enabled — the slot is physically attached *after* the body node's first `update` runs, so
 * the initial metadata cannot be applied by that update (the tab does not exist yet) and is set here
 * instead. Uninstall detaches by component identity (`remove(component)`, which the pane resolves to
 * the tab's current position), so removing an earlier tab first never invalidates a later tab's
 * uninstall. On every *subsequent* recomposition the body node's `update` block re-applies any changed
 * metadata, addressing the tab by its live `indexOfComponent`.
 */
private fun tabAttachment(
    title: String,
    icon: Icon?,
    tooltip: String?,
    enabled: Boolean,
): SlotAttachment =
    SlotAttachment { host, component, index ->
        host as JTabbedPane
        host.insertTab(title, icon, component, tooltip, index)
        host.setEnabledAt(host.indexOfComponent(component), enabled)
        // Detach by component, not by the install-time index: an earlier sibling may have been
        // removed first, shifting this tab down. JTabbedPane.remove(component) resolves the live
        // position via indexOfComponent and calls removeTabAt, releasing the page entirely.
        return@SlotAttachment { host.remove(component) }
    }
