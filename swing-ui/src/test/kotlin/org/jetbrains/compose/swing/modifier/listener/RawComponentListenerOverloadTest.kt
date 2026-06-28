package org.jetbrains.compose.swing.modifier.listener

import org.jetbrains.compose.swing.components.ComboBox
import org.jetbrains.compose.swing.components.Slider
import org.jetbrains.compose.swing.components.Spinner
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.button.CheckBox
import org.jetbrains.compose.swing.components.button.RadioButton
import org.jetbrains.compose.swing.components.button.ToggleButton
import org.jetbrains.compose.swing.components.layout.ScrollPane
import org.jetbrains.compose.swing.components.layout.SplitPane
import org.jetbrains.compose.swing.components.layout.TabbedPane
import org.jetbrains.compose.swing.components.selection.ListBox
import org.jetbrains.compose.swing.components.selection.Table
import org.jetbrains.compose.swing.components.selection.Tree
import org.jetbrains.compose.swing.components.text.EditorPane
import org.jetbrains.compose.swing.components.text.FormattedTextField
import org.jetbrains.compose.swing.components.text.PasswordField
import org.jetbrains.compose.swing.components.text.TextArea
import org.jetbrains.compose.swing.components.text.TextField
import org.jetbrains.compose.swing.components.text.TextPane
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.name
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.event.ActionListener
import java.beans.PropertyChangeListener
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JEditorPane
import javax.swing.JFormattedTextField
import javax.swing.JList
import javax.swing.JPasswordField
import javax.swing.JRadioButton
import javax.swing.JSlider
import javax.swing.JSpinner
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JTextPane
import javax.swing.JToggleButton
import javax.swing.JTree
import javax.swing.event.ChangeListener
import javax.swing.event.ListSelectionListener
import javax.swing.event.TreeSelectionListener
import javax.swing.text.AbstractDocument
import javax.swing.text.JTextComponent
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Behavioral coverage for the raw-listener composable overloads: each component that ships an
 * `onXxx` lambda also ships an overload taking the underlying Swing listener object. Each test asserts
 * the exact instance passed is registered on the live component through the matching `getXxxListeners()`
 * accessor (or, for the text components, on the field's document) — the observable proof the overload
 * wires the listener to the right registration site.
 */
class RawComponentListenerOverloadTest {
    private fun docListener(): javax.swing.event.DocumentListener = object : javax.swing.event.DocumentListener {
        override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = Unit

        override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = Unit

        override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = Unit
    }

    private fun JTextComponent.documentHas(listener: javax.swing.event.DocumentListener): Boolean {
        val document = document
        return document is AbstractDocument && document.documentListeners.any { it === listener }
    }

    @Test
    fun buttonActionListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ActionListener { }
        setContent { Button("X", actionListener = listener, modifier = SwingModifier.name("b")) }
        assertTrue(onNodeWithName("b").fetch<JButton>().actionListeners.any { it === listener })
    }

    @Test
    fun checkBoxActionListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ActionListener { }
        setContent { CheckBox("X", actionListener = listener, modifier = SwingModifier.name("c")) }
        assertTrue(onNodeWithName("c").fetch<JCheckBox>().actionListeners.any { it === listener })
    }

    @Test
    fun radioButtonActionListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ActionListener { }
        setContent { RadioButton("X", actionListener = listener, modifier = SwingModifier.name("r")) }
        assertTrue(onNodeWithName("r").fetch<JRadioButton>().actionListeners.any { it === listener })
    }

    @Test
    fun toggleButtonActionListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ActionListener { }
        setContent { ToggleButton("X", actionListener = listener, modifier = SwingModifier.name("t")) }
        assertTrue(onNodeWithName("t").fetch<JToggleButton>().actionListeners.any { it === listener })
    }

    @Test
    fun comboBoxActionListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ActionListener { }
        setContent {
            ComboBox(items = listOf("a", "b"), actionListener = listener, modifier = SwingModifier.name("cb"))
        }
        assertTrue(onNodeWithName("cb").fetch<JComboBox<*>>().actionListeners.any { it === listener })
    }

    @Test
    fun sliderChangeListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ChangeListener { }
        setContent { Slider(value = 5, changeListener = listener, modifier = SwingModifier.name("s")) }
        assertTrue(onNodeWithName("s").fetch<JSlider>().changeListeners.any { it === listener })
    }

    @Test
    fun intSpinnerChangeListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ChangeListener { }
        setContent { Spinner(value = 1, changeListener = listener, modifier = SwingModifier.name("si")) }
        assertTrue(onNodeWithName("si").fetch<JSpinner>().changeListeners.any { it === listener })
    }

    @Test
    fun doubleSpinnerChangeListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ChangeListener { }
        setContent { Spinner(value = 1.0, changeListener = listener, modifier = SwingModifier.name("sd")) }
        assertTrue(onNodeWithName("sd").fetch<JSpinner>().changeListeners.any { it === listener })
    }

    @Test
    fun listSpinnerChangeListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ChangeListener { }
        setContent {
            Spinner(items = listOf("a", "b"), changeListener = listener, modifier = SwingModifier.name("sl"))
        }
        assertTrue(onNodeWithName("sl").fetch<JSpinner>().changeListeners.any { it === listener })
    }

    @Test
    fun tabbedPaneChangeListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ChangeListener { }
        setContent {
            TabbedPane(selectedIndex = 0, changeListener = listener, modifier = SwingModifier.name("tp")) {
                tab("One") { }
            }
        }
        assertTrue(onNodeWithName("tp").fetch<JTabbedPane>().changeListeners.any { it === listener })
    }

    @Test
    fun splitPaneDividerListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = PropertyChangeListener { }
        setContent {
            SplitPane(dividerLocationListener = listener, modifier = SwingModifier.name("sp")) {
                first { }
                second { }
            }
        }
        val pane = onNodeWithName("sp").fetch<JSplitPane>()
        assertTrue(
            pane.getPropertyChangeListeners(JSplitPane.DIVIDER_LOCATION_PROPERTY).any { it === listener },
        )
    }

    @Test
    fun tableListSelectionListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ListSelectionListener { }
        setContent {
            ScrollPane {
                content {
                    Table(
                        rows = listOf("a", "b"),
                        listSelectionListener = listener,
                        modifier = SwingModifier.name("tbl"),
                    ) {
                        column("C") { it }
                    }
                }
            }
        }
        val table = onNodeWithName("tbl").fetch<JTable>()
        val model = table.selectionModel as javax.swing.DefaultListSelectionModel
        assertTrue(model.listSelectionListeners.any { it === listener })
    }

    @Test
    fun treeSelectionListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = TreeSelectionListener { }
        setContent {
            ScrollPane {
                content {
                    Tree(
                        root = "root",
                        children = { emptyList() },
                        treeSelectionListener = listener,
                        modifier = SwingModifier.name("tr"),
                    )
                }
            }
        }
        assertTrue(onNodeWithName("tr").fetch<JTree>().treeSelectionListeners.any { it === listener })
    }

    @Test
    fun listBoxSelectionListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = ListSelectionListener { }
        setContent {
            ScrollPane {
                content {
                    ListBox(
                        items = listOf("a", "b"),
                        listSelectionListener = listener,
                        modifier = SwingModifier.name("lb"),
                    )
                }
            }
        }
        assertTrue(onNodeWithName("lb").fetch<JList<*>>().listSelectionListeners.any { it === listener })
    }

    @Test
    fun formattedTextFieldValueListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = PropertyChangeListener { }
        setContent {
            FormattedTextField(
                value = 1,
                valuePropertyChangeListener = listener,
                modifier = SwingModifier.name("ftf"),
            )
        }
        val field = onNodeWithName("ftf").fetch<JFormattedTextField>()
        assertTrue(field.getPropertyChangeListeners("value").any { it === listener })
    }

    @Test
    fun textFieldDocumentListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = docListener()
        setContent { TextField("hi", documentListener = listener, modifier = SwingModifier.name("tf")) }
        assertTrue(onNodeWithName("tf").fetch<JTextField>().documentHas(listener))
    }

    @Test
    fun textAreaDocumentListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = docListener()
        setContent { TextArea("hi", documentListener = listener, modifier = SwingModifier.name("ta")) }
        assertTrue(onNodeWithName("ta").fetch<JTextArea>().documentHas(listener))
    }

    @Test
    fun passwordFieldDocumentListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = docListener()
        setContent {
            PasswordField(
                value = charArrayOf('a'),
                documentListener = listener,
                modifier = SwingModifier.name("pf"),
            )
        }
        assertTrue(onNodeWithName("pf").fetch<JPasswordField>().documentHas(listener))
    }

    @Test
    fun editorPaneDocumentListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = docListener()
        setContent { EditorPane("hi", documentListener = listener, modifier = SwingModifier.name("ep")) }
        assertTrue(onNodeWithName("ep").fetch<JEditorPane>().documentHas(listener))
    }

    @Test
    fun textPaneDocumentListenerOverloadRegistersInstance() = runSwingUiTest {
        val listener = docListener()
        setContent { TextPane("hi", documentListener = listener, modifier = SwingModifier.name("tpane")) }
        assertTrue(onNodeWithName("tpane").fetch<JTextPane>().documentHas(listener))
    }
}
