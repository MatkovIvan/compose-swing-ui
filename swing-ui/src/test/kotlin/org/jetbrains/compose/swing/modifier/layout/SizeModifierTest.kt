package org.jetbrains.compose.swing.modifier.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.desktop.LayeredPane
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.background
import org.jetbrains.compose.swing.modifier.appearance.foreground
import org.jetbrains.compose.swing.test.ComposeSwingTest
import org.jetbrains.compose.swing.test.onNodeOfType
import org.jetbrains.compose.swing.test.runComposeSwingTest
import java.awt.Color
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JLayeredPane
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Behavioral tests for the actual-size modifiers (`size`/`width`/`height`). They set the component's
 * live size (`setSize`), which a layout manager would override on its next pass, so each test hosts the
 * modified child in a [JLayeredPane] - a parent with no layout manager - where the set size persists.
 * The assertions read what an observer of the live Swing component sees: its `getSize`/`getWidth`/
 * `getHeight`.
 */
class SizeModifierTest {
    /** Hosts a single [LayeredPane] child carrying [modifier] and returns its live component. */
    private fun ComposeSwingTest.sizedChild(modifier: SwingModifier): JLabel {
        setContent {
            LayeredPane {
                Label(text = "child", modifier = modifier.layer(JLayeredPane.DEFAULT_LAYER))
            }
        }
        return onNodeOfType<JLabel>().fetch()
    }

    @Test
    fun sizeSetsTheActualSize() = runComposeSwingTest {
        val child = sizedChild(SwingModifier.size(120, 40))
        assertEquals(Dimension(120, 40), child.size, "size should set the component's actual size")
    }

    @Test
    fun sizeDimensionOverloadSetsTheActualSize() = runComposeSwingTest {
        val child = sizedChild(SwingModifier.size(Dimension(90, 30)))
        assertEquals(Dimension(90, 30), child.size, "the Dimension overload should set the actual size")
    }

    @Test
    fun theSizeDeclarationNamesEachAxisItsWriteCovers() {
        val held =
            SwingModifier.size(120, 40).foldIn(emptySet<String>()) { names, element ->
                names + (element as SwingModifier.NodeElement<*, *>).heldProperties
            }

        assertEquals(setOf("size", "width", "height"), held, "one write, every axis it lands on named")
    }

    @Test
    fun widthSetsTheWidthKeepingHeight() = runComposeSwingTest {
        // Establish a known height first, then narrow the width: the height must survive.
        val child = sizedChild(SwingModifier.size(50, 33).width(150))
        assertEquals(150, child.width, "width should set the actual width")
        assertEquals(33, child.height, "width must keep the current height")
    }

    @Test
    fun heightSetsTheHeightKeepingWidth() = runComposeSwingTest {
        val child = sizedChild(SwingModifier.size(44, 50).height(55))
        assertEquals(55, child.height, "height should set the actual height")
        assertEquals(44, child.width, "height must keep the current width")
    }

    @Test
    fun widthAndHeightCombineIntoAFullSize() = runComposeSwingTest {
        val child = sizedChild(SwingModifier.width(10).height(20))
        // Each axis comes from its own modifier; they combine into a full size.
        assertEquals(Dimension(10, 20), child.size, "width+height combine into a full size")
    }

    @Test
    fun heightThenWidthAlsoCombinesRegardlessOfOrder() = runComposeSwingTest {
        val child = sizedChild(SwingModifier.height(20).width(10))
        assertEquals(Dimension(10, 20), child.size, "distinct axes combine regardless of order")
    }

    @Test
    fun sizeAfterWidthWinsTheWidthAxis() = runComposeSwingTest {
        val child = sizedChild(SwingModifier.width(10).size(20, 30))
        // size is applied later in the modifier chain, so its width wins over the earlier width(10).
        assertEquals(Dimension(20, 30), child.size, "later size wins the width axis")
    }

    @Test
    fun widthAfterSizeWinsTheWidthAxis() = runComposeSwingTest {
        val child = sizedChild(SwingModifier.size(20, 30).width(10))
        // width is applied later, so it wins the width axis; the height axis stays from size.
        assertEquals(Dimension(10, 30), child.size, "later width wins the width axis, height stays from size")
    }

    @Test
    fun droppingAWholeSizeAndThenAnAxisLeavesTheChildWhereItStood() = runComposeSwingTest {
        // The axis slot joins the modifier over a width the size slot has already written, so what it puts
        // back has to be the width the child came with rather than the one it read on the way in.
        var declared by mutableStateOf(0)
        setContent {
            LayeredPane {
                val onDefaultLayer = SwingModifier.layer(JLayeredPane.DEFAULT_LAYER)
                Label(
                    text = "child",
                    modifier =
                        when (declared) {
                            1 -> onDefaultLayer.size(120, 40).width(10)
                            2 -> onDefaultLayer.width(10)
                            else -> onDefaultLayer
                        },
                )
            }
        }
        val child = onNodeOfType<JLabel>().fetch()
        val stood = child.size

        declared = 1
        awaitIdle()
        assertEquals(Dimension(10, 40), child.size, "the width is declared last and wins its axis")

        // The whole size goes first, leaving the axis standing on what that size wrote.
        declared = 2
        awaitIdle()
        declared = 3
        awaitIdle()

        assertEquals(stood, child.size, "the child is left at the size it stood at before either declaration")
    }

    @Test
    fun removingTheSizeModifierRestoresTheOriginalSize() = runComposeSwingTest {
        var sized by mutableStateOf(true)
        setContent {
            LayeredPane {
                val onDefaultLayer = SwingModifier.layer(JLayeredPane.DEFAULT_LAYER)
                Label(
                    text = "child",
                    modifier = if (sized) onDefaultLayer.size(120, 40) else onDefaultLayer,
                )
            }
        }
        val sizedComponent = onNodeOfType<JLabel>().fetch()
        // The size the component had before the modifier captured it - its pre-modifier original.
        assertEquals(Dimension(120, 40), sizedComponent.size, "size should set the actual size while applied")

        sized = false
        awaitIdle()
        // The size declaration is gone, so the component returns to the size it had before it.
        val restored = onNodeOfType<JLabel>().fetch()
        assertEquals(
            Dimension(0, 0),
            restored.size,
            "removing the size modifier restores the component's original size",
        )
    }

    @Test
    fun addingTheSizeLeavesTheAxisAnotherModifierDeclares() = runComposeSwingTest {
        var whole by mutableStateOf(false)
        setContent {
            LayeredPane {
                val onDefaultLayer = SwingModifier.layer(JLayeredPane.DEFAULT_LAYER)
                Label(
                    text = "child",
                    modifier = (if (whole) onDefaultLayer.size(120, 40) else onDefaultLayer).width(10),
                )
            }
        }
        val child = onNodeOfType<JLabel>().fetch()
        assertEquals(10, child.width, "the declared width is what the child carries")

        // The size arrives ahead of the width declaration and writes the axis it declares, so the
        // later declaration is written again over it.
        whole = true
        awaitIdle()

        assertEquals(10, child.width, "the declared width stands after the size arrives under it")
        assertEquals(40, child.height, "the size reaches the axis it owns")
    }

    @Test
    fun droppingTheSizeLeavesTheAxisAnotherModifierDeclares() = runComposeSwingTest {
        var whole by mutableStateOf(true)
        setContent {
            LayeredPane {
                val onDefaultLayer = SwingModifier.layer(JLayeredPane.DEFAULT_LAYER)
                Label(
                    text = "child",
                    modifier = (if (whole) onDefaultLayer.size(120, 40) else onDefaultLayer).width(10),
                )
            }
        }
        val child = onNodeOfType<JLabel>().fetch()
        assertEquals(Dimension(10, 40), child.size, "the later width wins its axis, height stays from size")

        whole = false
        awaitIdle()

        // Restoring a size puts both axes back, width included, so the modifier declaring that axis
        // alone writes it again on the same pass.
        assertEquals(Dimension(10, 0), child.size, "the declared width stands after the size is dropped")
    }

    @Test
    fun sizeReactsToStateChangeAcrossRecomposition() = runComposeSwingTest {
        var wide by mutableStateOf(true)
        setContent {
            LayeredPane {
                Label(
                    text = "child",
                    modifier = SwingModifier.layer(JLayeredPane.DEFAULT_LAYER).size(if (wide) 120 else 60, 40),
                )
            }
        }
        val label = onNodeOfType<JLabel>()
        assertEquals(
            Dimension(120, 40),
            label.fetch().size,
            "size should set the actual size before the state changes",
        )

        wide = false
        awaitIdle()
        assertEquals(
            Dimension(60, 40),
            label.fetch().size,
            "size should react to the state change",
        )
    }

    @Test
    fun theLastPlaceAKeyIsDeclaredAtIsWhereItStands() = runComposeSwingTest {
        setContent {
            LayeredPane {
                Label(
                    text = "child",
                    modifier =
                        SwingModifier
                            .layer(JLayeredPane.DEFAULT_LAYER)
                            .width(10)
                            .size(20, 30)
                            .width(5),
                )
            }
        }
        assertEquals(
            Dimension(5, 30),
            onNodeOfType<JLabel>().fetch().size,
            "a key declared again later should stand where it was declared last",
        )
    }

    @Test
    fun swappingTwoDeclarationsOfOneAxisWritesTheOneNowDeclaredLast() = runComposeSwingTest {
        var sizeFirst by mutableStateOf(true)
        setContent {
            LayeredPane {
                val onDefaultLayer = SwingModifier.layer(JLayeredPane.DEFAULT_LAYER)
                Label(
                    text = "child",
                    modifier =
                        onDefaultLayer then
                            if (sizeFirst) {
                                // Two pairs swap places, so only writing from the first of them puts
                                // the geometry where the order declares it.
                                SwingModifier
                                    .size(20, 30)
                                    .width(10)
                                    .foreground(Color.RED)
                                    .background(Color.BLUE)
                            } else {
                                SwingModifier
                                    .width(10)
                                    .size(20, 30)
                                    .background(Color.BLUE)
                                    .foreground(Color.RED)
                            },
                )
            }
        }
        val child = onNodeOfType<JLabel>().fetch()
        assertEquals(Dimension(10, 30), child.size, "the width is declared last and wins its axis")

        // No declaration changed, so nothing but their order says which one stands.
        sizeFirst = false
        awaitIdle()

        assertEquals(Dimension(20, 30), child.size, "the size is declared last now and wins the width axis")
    }
}
