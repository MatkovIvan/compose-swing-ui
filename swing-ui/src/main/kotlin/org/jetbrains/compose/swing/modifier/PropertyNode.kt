package org.jetbrains.compose.swing.modifier

import java.awt.Component

/**
 * Stable per-property slot keys for the shared [PropertyElement]. Each built-in property gets its own
 * constant so two different properties (which share the [PropertyElement] runtime class) never collapse
 * into one last-wins slot. Keyed escape hatches (a client property) use their own key instead.
 */
internal enum class PropertyKey {
    FOREGROUND,
    BACKGROUND,
    FONT,
    BORDER,
    OPAQUE,
    CURSOR,
    BOUNDS,
    PREFERRED_SIZE,
    MINIMUM_SIZE,
    MAXIMUM_SIZE,
    ALIGNMENT_X,
    ALIGNMENT_Y,
    VISIBLE,
    COMPONENT_ORIENTATION,
    NAME,
    TEST_TAG,
    TOOL_TIP,
    ACCESSIBLE_NAME,
    ACCESSIBLE_DESCRIPTION,
    ACCESSIBLE_ROLE,
    MNEMONIC,
    FOCUS_TRAVERSAL_INDEX,
    ORDERED_FOCUS_TRAVERSAL,
    FOCUSABLE,
    ENABLED,
}

/**
 * A [SwingModifier.Node] for a single component property. On [onAttach] it captures the property's
 * pre-modifier value as a restore action; on each apply it writes the latest value; on [onDetach] it
 * runs the captured restore. [read] reads the current value (for capture), [write] applies a value,
 * and [valueProvider] supplies the value to write — refreshed by the owning element's `update`.
 *
 * This is the shape every appearance/layout/metadata/accessibility property element shares: capture
 * once, write the new value, restore on removal. The restore is held as a closure over the captured
 * value, so no value is stored or cast back through erasure.
 */
internal class PropertyNode<T : Component, V>(
    private val read: (component: T) -> V,
    private val write: (component: T, value: V) -> Unit,
) : SwingModifier.Node<T>() {
    /** Supplies the value to write, rebound by the owning element's `update` to the latest data. */
    var valueProvider: () -> V = { error("PropertyNode value was not set before apply()") }

    private var restore: (() -> Unit)? = null

    override fun onAttach() {
        val component = component
        val original = read(component)
        restore = { write(component, original) }
    }

    /** Writes the latest value; call from the owning element's `update`. */
    fun apply(): Unit = write(component, valueProvider())

    override fun onDetach() {
        restore?.invoke()
    }
}

/**
 * A [SwingModifier.Element] for a single component property, backed by a [PropertyNode]. Holds the
 * [value] to write plus the property's [read]/[write] accessors and a [slotKey] identifying the
 * property so distinct properties (and distinct client-property keys) are independent last-wins slots.
 * [create] builds the node; [update] rebinds the node's value provider to this element's [value] and
 * writes it.
 */
internal class PropertyElement<T : Component, V>(
    override val targetType: Class<T>,
    private val slotKey: Any,
    private val value: V,
    private val read: (component: T) -> V,
    private val write: (component: T, value: V) -> Unit,
) : SwingModifier.Element<T, PropertyNode<T, V>> {
    override val key: Any get() = slotKey

    override fun create(): PropertyNode<T, V> = PropertyNode(read, write)

    override fun update(node: PropertyNode<T, V>) {
        node.valueProvider = { value }
        node.apply()
    }
}

/**
 * Builds a keyed property [SwingModifier.Element] over [PropertyNode] from a read/write pair. [slotKey]
 * identifies the property (one per built-in property; a client-property key for keyed escape hatches),
 * so two different properties never collapse into one last-wins slot.
 */
internal inline fun <reified T : Component, V> propertyElement(
    slotKey: Any,
    value: V,
    noinline read: (component: T) -> V,
    noinline write: (component: T, value: V) -> Unit,
): SwingModifier.Element<T, PropertyNode<T, V>> = PropertyElement(T::class.java, slotKey, value, read, write)
