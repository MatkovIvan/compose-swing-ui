package org.jetbrains.compose.swing.modifier

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.node.SwingNode
import org.jetbrains.compose.swing.test.onNodeOfType
import org.jetbrains.compose.swing.test.runComposeSwingTest
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JLabel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The public builder a custom component declares a Swing property through: it writes the declared value
 * while the declaration stands, and puts back what the component was found holding once it leaves.
 *
 * A caller states through `restores` whether the component is held to carrying that value again,
 * and may state it differently from one pass to the next. Both statements write through one accessor
 * and so take one slot.
 */
class PropertyTest {
    @Test
    fun aDeclaredPropertyIsWrittenAndHandedBackFromTheWidget() = runComposeSwingTest {
        var declared by mutableStateOf(true)
        setContent {
            SwingNode(
                factory = { JLabel("target").apply { toolTipText = WIDGET_TIP } },
                modifier = if (declared) SwingModifier.declaredTip(DECLARED_TIP) else SwingModifier,
            )
        }
        val label = onNodeOfType<JLabel>()
        assertEquals(DECLARED_TIP, label.fetch().toolTipText, "the declared value should reach the widget")

        declared = false
        awaitIdle()
        assertEquals(
            WIDGET_TIP,
            label.fetch().toolTipText,
            "withdrawing the declaration should hand back what the widget was carrying",
        )
    }

    @Test
    fun aPropertyThatStartsRestoringPartWayThroughIsBuiltAgainOnItsSlot() = runComposeSwingTest {
        // Both declarations write through the same accessor, so both take the same slot. They are not
        // the same kind of element, and the slot's node was built for the one that arrived first: it
        // cannot be handed the other, so the slot comes apart and is built again - observed here by a
        // read that counts every fresh attach's capture of the widget's own value, since only an attach
        // reads it at all.
        var restoring by mutableStateOf(false)
        var tip by mutableStateOf(DECLARED_TIP)
        val attaches = AtomicInteger()
        val countingRead: (component: JLabel) -> String? = {
            attaches.incrementAndGet()
            it.toolTipText
        }
        setContent {
            SwingNode(
                factory = { JLabel("target").apply { toolTipText = WIDGET_TIP } },
                modifier =
                    if (restoring) {
                        SwingModifier.declaredTip(tip, countingRead)
                    } else {
                        SwingModifier.keptTip(tip, countingRead)
                    },
            )
        }
        val label = onNodeOfType<JLabel>()
        assertEquals(DECLARED_TIP, label.fetch().toolTipText, "the declared value should reach the widget")
        assertEquals(1, attaches.get(), "the slot attaches once on the first apply")

        restoring = true
        awaitIdle()
        assertEquals(
            2,
            attaches.get(),
            "the slot's node was built for the element that arrived first, so a policy change rebuilds it",
        )
        assertEquals(
            DECLARED_TIP,
            label.fetch().toolTipText,
            "the declaration standing after the change should still be written",
        )

        // A pass after the change is what says the change was survived: a slot handed an element it
        // cannot host fails inside the apply, which ends the composition applying it, and nothing
        // declared afterwards would reach the widget at all.
        tip = LATER_TIP
        awaitIdle()
        assertEquals(
            LATER_TIP,
            label.fetch().toolTipText,
            "a value declared after the change should reach the widget, which needs the composition alive",
        )
    }

    @Test
    fun aDeclaredPropertyOnlyPolicyOwesBackOnlyItsOwnName() = runComposeSwingTest {
        var declared by mutableStateOf(true)
        setContent {
            SwingNode(
                factory = {
                    JLabel("target").apply {
                        toolTipText = WIDGET_TIP
                        name = WIDGET_NAME
                    }
                },
                modifier = if (declared) SwingModifier.declaredTipDerivingName(DECLARED_TIP) else SwingModifier,
            )
        }
        val label = onNodeOfType<JLabel>().fetch()
        assertEquals(DECLARED_TIP, label.toolTipText, "the declared value should reach the widget")

        declared = false
        // The write derives `name` from the tooltip too, the way a look and feel derives a property of
        // its own from a write. RestorePolicy.DeclaredPropertyOnly holds the departing slot to its own
        // name ("toolTipText") alone, so the restore check the harness runs does not fail even though
        // `name` is left standing at what the write derived rather than at what it carried before.
        awaitIdle()
        assertEquals(WIDGET_TIP, label.toolTipText, "the declared property is still put back")
        assertEquals(
            "derived from $WIDGET_TIP",
            label.name,
            "a property the policy does not own is left as the write derived it, not restored",
        )
    }
}

/**
 * The accessor both declarations here write through, written out once so they share one slot, the way
 * the builder's documentation directs a custom component to declare a property.
 */
private val writeToolTip: (component: JLabel, value: String?) -> Unit = { label, value -> label.toolTipText = value }

private val readToolTip: (component: JLabel) -> String? = { it.toolTipText }

private fun SwingModifier.declaredTip(
    tip: String,
    read: (component: JLabel) -> String? = readToolTip,
): SwingModifier = property("toolTipText", tip, read, writeToolTip)

private fun SwingModifier.keptTip(
    tip: String,
    read: (component: JLabel) -> String? = readToolTip,
): SwingModifier = property("toolTipText", tip, read, writeToolTip, restores = RestorePolicy.None)

/**
 * Writes the tooltip and, standing in for a look and feel that derives a property of its own from a
 * write, derives the component's name from it too.
 */
private val writeToolTipDerivingName: (component: JLabel, value: String?) -> Unit = { label, value ->
    label.toolTipText = value
    label.name = "derived from $value"
}

private fun SwingModifier.declaredTipDerivingName(tip: String): SwingModifier = property(
    "toolTipText",
    tip,
    readToolTip,
    writeToolTipDerivingName,
    restores = RestorePolicy.DeclaredPropertyOnly,
)

/** What the widget is built carrying, so a restore has something of the widget's own to hand back. */
private const val WIDGET_TIP = "carried by the widget"

private const val DECLARED_TIP = "named by the declaration"

/** A second declared value, so a pass after the slot is rebuilt has something new to write. */
private const val LATER_TIP = "named by a later declaration"

/** What the widget is built named, so leaving a derived name standing is a difference from this. */
private const val WIDGET_NAME = "named by the widget"
