@file:JvmMultifileClass
@file:JvmName("DesktopComponentsKt")

package org.jetbrains.compose.swing.components.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import org.jetbrains.compose.swing.SlotNode
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.components.SyntheticIds
import org.jetbrains.compose.swing.core.SlotAttachment
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import javax.swing.JLayeredPane

/**
 * A composable wrapper for `JLayeredPane` that stacks children on integer depth layers.
 *
 * Declare the children you need in [block]; each `layer(z) { … }` places its child at depth `z`. Higher
 * layers paint above lower ones, and within a layer later-declared children paint above earlier ones.
 * Children are positioned by their own bounds (`SwingModifier.bounds(...)`), since a layered pane does
 * not lay its children out.
 *
 * ```
 * LayeredPane {
 *     layer(JLayeredPane.DEFAULT_LAYER) { Background() }
 *     layer(JLayeredPane.PALETTE_LAYER) { FloatingToolbar() }
 * }
 * ```
 *
 * Adding or removing a `layer(...)` in the composition adds or removes the matching child, and a
 * child's layer updates on recomposition.
 *
 * @param modifier the [SwingModifier] applied to the underlying `JLayeredPane`
 * @param block declares the layered children; see [LayeredPaneScope]
 */
@Composable
public fun LayeredPane(
    modifier: SwingModifier = SwingModifier,
    block: LayeredPaneScope.() -> Unit,
) {
    // Collect the child declarations fresh on every composition so a child the caller stops declaring
    // (e.g. behind an `if`) drops out of `content`, and the applier uninstalls it via the slot
    // mechanism. A remembered, mutated scope would retain the stale declaration.
    val scope = LayeredPaneScopeImpl().apply(block)
    val pane = remember { JLayeredPane() }

    SwingNode(
        factory = { pane },
        update = {
            applyModifier(modifier)
        },
        content = {
            scope.children.forEach { child ->
                // key() gives each child a composition identity by (synthetic id, layer): the child
                // keeps its slot — and the state inside it — even if the surrounding declarations shift
                // around it, and changing a child's layer re-keys it so the applier uninstalls it from
                // the old depth and installs it fresh at the new one.
                key(child.id, child.layer) {
                    val attachment = remember(child.layer) { layerAttachment(child.layer) }
                    SlotNode(attachment) {
                        child.content()
                    }
                }
            }
        },
    )
}

/**
 * One declared child: a synthetic [id] stable across recompositions, the depth layer it sits on, plus
 * its composable.
 */
private class LayerDeclaration(
    val id: Int,
    val layer: Int,
    val content: @Composable () -> Unit,
)

private class LayeredPaneScopeImpl : LayeredPaneScope {
    val children: MutableList<LayerDeclaration> = ArrayList()

    private val ids = SyntheticIds()

    override fun layer(
        layer: Int,
        content: @Composable () -> Unit,
    ) {
        children.add(LayerDeclaration(ids.next(), layer, content))
    }
}

/**
 * Hosts one child of [pane] on the depth layer [layer] via `add(component, layer)`; uninstall detaches
 * it by component identity so removing an earlier child never invalidates a later child's uninstall.
 */
private fun layerAttachment(layer: Int): SlotAttachment =
    SlotAttachment { host, component, _ ->
        host as JLayeredPane
        // Record the depth on the component before attaching it, then add it: setLayer stamps the
        // layer client-property (the parent is still null, so it only stores), and the subsequent add
        // positions the component within that layer.
        host.setLayer(component, layer)
        host.add(component)
        return@SlotAttachment { host.remove(component) }
    }
