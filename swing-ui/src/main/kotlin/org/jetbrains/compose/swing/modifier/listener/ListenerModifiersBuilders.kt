@file:JvmMultifileClass
@file:JvmName("ListenerModifiersKt")

package org.jetbrains.compose.swing.modifier.listener

import org.jetbrains.compose.swing.modifier.SwingModifier
import java.awt.Component
import java.awt.event.ActionListener
import java.beans.PropertyChangeListener
import javax.swing.AbstractButton
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

/*
 * Typed instance builders for model- and role-specific listeners — property change, button action, and
 * text-document. They share the by-identity add/remove contract of the builders in ListenerModifiers.kt:
 * the instance is added once, the same instance is removed on detach, and supplying a different instance
 * on recomposition detaches the old one and attaches the new.
 */

/**
 * Attaches an unbound [PropertyChangeListener] (`addPropertyChangeListener`), notified of every bound
 * property change. For a single property, prefer the [name][propertyChangeListener] overload.
 */
public fun SwingModifier.propertyChangeListener(listener: PropertyChangeListener): SwingModifier =
    listener<Component, PropertyChangeListener>(
        listener,
        { c, l -> c.addPropertyChangeListener(l) },
        { c, l -> c.removePropertyChangeListener(l) },
    )

/**
 * Attaches a [PropertyChangeListener] bound to the property [name]
 * (`addPropertyChangeListener(name, listener)`), notified only of changes to that property.
 */
public fun SwingModifier.propertyChangeListener(
    name: String,
    listener: PropertyChangeListener,
): SwingModifier =
    listener<Component, PropertyChangeListener>(
        listener,
        { c, l -> c.addPropertyChangeListener(name, l) },
        { c, l -> c.removePropertyChangeListener(name, l) },
    )

/**
 * Attaches an [ActionListener] (`addActionListener`/`removeActionListener`). Requires an
 * [AbstractButton] target (`JButton`, `JCheckBox`, …).
 */
public fun SwingModifier.actionListener(listener: ActionListener): SwingModifier =
    listener<AbstractButton, ActionListener>(
        listener,
        { c, l -> c.addActionListener(l) },
        { c, l -> c.removeActionListener(l) },
    )

/**
 * Attaches a [DocumentListener] to the text component's `document` (`document.addDocumentListener`).
 * Requires a [JTextComponent] target (`JTextField`, `JTextArea`, …). The listener observes the
 * `document` the component holds at install time.
 */
public fun SwingModifier.documentListener(listener: DocumentListener): SwingModifier =
    listener<JTextComponent, DocumentListener>(
        listener,
        { c, l -> c.document.addDocumentListener(l) },
        { c, l -> c.document.removeDocumentListener(l) },
    )
