@file:JvmMultifileClass
@file:JvmName("AccessibilityModifiersKt")
@file:OptIn(InternalSwingUiApi::class)

package org.jetbrains.compose.swing.modifier

import org.jetbrains.compose.swing.annotations.InternalSwingUiApi
import org.jetbrains.compose.swing.modifier.appearance.TEST_TAG_CLIENT_PROPERTY_KEY
import java.awt.Component
import java.awt.Container
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingUtilities

/**
 * Marks this label as the caption for the component tagged [targetTag] (via
 * [testTag][SwingModifier.testTag]), wiring `JLabel.setLabelFor` so the label's mnemonic moves focus
 * to that component and assistive technologies read the two as a pair. The association is resolved
 * against the surrounding window once the tree is laid out. Requires a `JLabel` target.
 *
 * @param targetTag the [testTag][SwingModifier.testTag] of the component this label captions.
 */
public fun SwingModifier.labelFor(targetTag: String): SwingModifier = this then LabelForElement(targetTag)

private class LabelForElement(
    private val targetTag: String,
) : SwingModifier.Element<JLabel, LabelForElement.Node> {
    override val targetType: Class<JLabel> get() = JLabel::class.java

    override fun create(): Node = Node()

    override fun update(node: Node) {
        node.targetTag = targetTag
        node.apply()
    }

    class Node : SwingModifier.Node<JLabel>() {
        var targetTag: String = ""

        fun apply() {
            val label = component
            val tag = targetTag
            // Resolve once the current layout batch has attached both the label and its target.
            // Re-resolve on every apply so the association tracks a target that appears or moves
            // between recompositions.
            SwingUtilities.invokeLater {
                label.labelFor = findTaggedTarget(label, tag)
            }
        }

        override fun onDetach() {
            component.labelFor = null
        }
    }
}

/**
 * Walks up from [label] to the surrounding root container and searches its subtree for the single
 * `JComponent` tagged [tag] via [testTag][SwingModifier.testTag], returning it or `null` when absent.
 */
private fun findTaggedTarget(
    label: JLabel,
    tag: String,
): Component? {
    var root: Component = label
    while (root.parent != null) root = root.parent

    fun search(component: Component): Component? {
        if (component is JComponent && component.getClientProperty(TEST_TAG_CLIENT_PROPERTY_KEY) == tag) {
            return component
        }
        return (component as? Container)?.components?.firstNotNullOfOrNull(::search)
    }
    return search(root)
}
