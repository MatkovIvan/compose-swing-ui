package org.jetbrains.compose.swing.samples.widgets.layout

import org.jetbrains.compose.swing.test.ComposeSwingTest
import org.jetbrains.compose.swing.test.onAllNodesOfType
import javax.swing.JComboBox
import javax.swing.JSlider

internal fun ComposeSwingTest.layoutParameterSelector(name: String): JComboBox<*> =
    onAllNodesOfType<JComboBox<*>>()
        .fetchAll<JComboBox<*>>()
        .single { it.accessibleContext.accessibleName == name }

internal fun ComposeSwingTest.sliderNamed(name: String): JSlider =
    onAllNodesOfType<JSlider>()
        .fetchAll<JSlider>()
        .single { it.accessibleContext.accessibleName == name }
