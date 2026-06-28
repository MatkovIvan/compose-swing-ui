package org.jetbrains.compose.swing.modifier.appearance

import org.jetbrains.compose.swing.annotations.InternalSwingUiApi
import org.jetbrains.compose.swing.modifier.PropertyKey
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.propertyElement
import java.awt.Component
import javax.swing.JComponent

/*
 * Metadata SwingModifiers — auxiliary data that does not change appearance or layout: the component
 * name, tooltip, look-and-feel client properties, and the test tag.
 */

/** Sets `name` — the key components are looked up by in tests and automation; `null` clears it. */
public fun SwingModifier.name(name: String?): SwingModifier =
    this then
        propertyElement<Component, String?>(
            PropertyKey.NAME,
            name,
            read = { it.name },
            write = { c, v -> c.name = v },
        )

/**
 * Tags the component with [tag] so it can be located in tests independently of its name.
 *
 * @param tag the identifier used to find the component.
 */
public fun SwingModifier.testTag(tag: String): SwingModifier =
    this then
        propertyElement<JComponent, Any?>(
            PropertyKey.TEST_TAG,
            tag,
            read = { it.getClientProperty(TEST_TAG_CLIENT_PROPERTY_KEY) },
            write = { c, v -> c.putClientProperty(TEST_TAG_CLIENT_PROPERTY_KEY, v) },
        )

/**
 * The `JComponent` client-property key under which [testTag] stores its tag. The test harness reads
 * this key to resolve a tagged component; it is not intended for application use.
 */
@InternalSwingUiApi
public val TEST_TAG_CLIENT_PROPERTY_KEY: Any = "org.jetbrains.compose.swing.testTag"

/** Sets `toolTipText`; `null` clears the tooltip. Requires a `JComponent` target. */
public fun SwingModifier.toolTip(text: String?): SwingModifier =
    this then
        propertyElement<JComponent, String?>(
            PropertyKey.TOOL_TIP,
            text,
            read = { it.toolTipText },
            write = { c, v -> c.toolTipText = v },
        )

/**
 * Sets a `putClientProperty` entry — the escape hatch for look-and-feel styling keys (e.g. FlatLaf)
 * and accessibility hints. Each distinct [key] is an independent modifier slot; `null` restores the
 * value the component had before. Requires a `JComponent` target.
 */
public fun SwingModifier.clientProperty(
    key: Any,
    value: Any?,
): SwingModifier =
    this then
        // Key the slot by the client-property key so distinct client properties are independent slots.
        // A PropertyKey enum constant (used by every built-in property) never equals such a key, so no
        // collision with fixed-property elements is possible.
        propertyElement<JComponent, Any?>(
            key,
            value,
            read = { it.getClientProperty(key) },
            write = { c, v -> c.putClientProperty(key, v) },
        )
