package org.jetbrains.compose.swing.components

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.swing.COMPOSITION_KEY
import org.jetbrains.compose.swing.GlobalRecomposer
import org.jetbrains.compose.swing.GlobalSnapshotManager
import org.jetbrains.compose.swing.SwingApplier
import org.jetbrains.compose.swing.SwingDispatcher
import java.awt.Component
import javax.swing.*

/**
 * Sets the composable content of a JMenuBar.
 * This is similar to setContent in Compose Multiplatform.
 * 
 * Uses the global Recomposer and supports parent composition lookup via client properties.
 *
 * @param content the composable content to set
 */
fun JMenuBar.setContent(content: @Composable () -> Unit) {
    // Find parent composition by traversing the Swing tree
    val parentComposition = findParentComposition()
    
    val recomposer = GlobalRecomposer.get()
    val applier = MenuBarApplier(this)
    val composition = if (parentComposition != null) {
        Composition(applier, parentComposition)
    } else {
        Composition(applier, recomposer)
    }
    
    // Store the composition in client properties for child lookups
    putClientProperty(COMPOSITION_KEY, composition)
    
    composition.setContent(content)
}

/**
 * Finds the parent composition by traversing up the Swing component tree.
 */
private fun JMenuBar.findParentComposition(): CompositionContext? {
    var current: Component? = parent
    while (current != null) {
        if (current is JComponent) {
            val composition = current.getClientProperty(COMPOSITION_KEY)
            if (composition is CompositionContext) {
                return composition
            }
        }
        current = current.parent
    }
    return null
}

/**
 * Custom applier for JMenuBar that handles menu items.
 */
private class MenuBarApplier(root: JMenuBar) : AbstractApplier<Any>(root) {
    
    override fun insertTopDown(index: Int, instance: Any) {
        // Menu items are inserted bottom-up
    }

    override fun insertBottomUp(index: Int, instance: Any) {
        when (val currentNode = current) {
            is JMenuBar -> {
                if (instance is JMenu) {
                    currentNode.add(instance, index)
                }
            }
            is JMenu -> {
                when (instance) {
                    is JMenuItem -> currentNode.add(instance, index)
                    is Component -> currentNode.add(instance, index)
                }
            }
            is JPopupMenu -> {
                when (instance) {
                    is JMenuItem -> currentNode.add(instance, index)
                    is Component -> currentNode.add(instance, index)
                }
            }
        }
    }

    override fun remove(index: Int, count: Int) {
        when (val currentNode = current) {
            is JMenuBar -> repeat(count) { currentNode.remove(index) }
            is JMenu -> repeat(count) { currentNode.remove(index) }
            is JPopupMenu -> repeat(count) { currentNode.remove(index) }
        }
    }

    override fun move(from: Int, to: Int, count: Int) {
        // Moving menu items is complex and rarely needed, so we skip it for now
    }

    override fun onClear() {
        when (val currentNode = root) {
            is JMenuBar -> currentNode.removeAll()
            is JMenu -> currentNode.removeAll()
        }
    }
}

/**
 * A composable wrapper for JMenu.
 *
 * @param text the text of the menu
 * @param enabled whether the menu is enabled
 * @param content the composable content of the menu (menu items)
 */
@Composable
fun Menu(
    text: String,
    enabled: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    val menu = remember { JMenu(text) }
    
    ComposeNode<Any, androidx.compose.runtime.Applier<Any>>(
        factory = { menu },
        update = {
            set(text) { menu.text = it }
            set(enabled) { menu.isEnabled = it }
        }
    ) {
        content()
    }
}

/**
 * A composable wrapper for JMenuItem.
 *
 * @param text the text of the menu item
 * @param enabled whether the menu item is enabled
 * @param onClick callback to be invoked when the menu item is clicked
 */
@Composable
fun MenuItem(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val menuItem = remember { JMenuItem(text) }
    
    ComposeNode<Any, androidx.compose.runtime.Applier<Any>>(
        factory = { menuItem },
        update = {
            set(text) { menuItem.text = it }
            set(enabled) { menuItem.isEnabled = it }
            set(onClick) { 
                menuItem.actionListeners.forEach { menuItem.removeActionListener(it) }
                menuItem.addActionListener { onClick() }
            }
        }
    )
}

/**
 * A composable wrapper for JCheckBoxMenuItem.
 *
 * @param text the text of the menu item
 * @param checked whether the menu item is checked
 * @param enabled whether the menu item is enabled
 * @param onCheckedChange callback invoked when the checked state changes
 */
@Composable
fun CheckBoxMenuItem(
    text: String,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    val menuItem = remember { JCheckBoxMenuItem(text) }
    
    ComposeNode<Any, androidx.compose.runtime.Applier<Any>>(
        factory = { menuItem },
        update = {
            set(text) { menuItem.text = it }
            set(checked) { menuItem.isSelected = it }
            set(enabled) { menuItem.isEnabled = it }
            set(onCheckedChange) { 
                menuItem.actionListeners.forEach { menuItem.removeActionListener(it) }
                menuItem.addActionListener { onCheckedChange(menuItem.isSelected) }
            }
        }
    )
}

/**
 * A composable wrapper for JRadioButtonMenuItem.
 *
 * @param text the text of the menu item
 * @param selected whether the menu item is selected
 * @param enabled whether the menu item is enabled
 * @param onSelect callback invoked when the menu item is selected
 */
@Composable
fun RadioButtonMenuItem(
    text: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onSelect: () -> Unit = {}
) {
    val menuItem = remember { JRadioButtonMenuItem(text) }
    
    ComposeNode<Any, androidx.compose.runtime.Applier<Any>>(
        factory = { menuItem },
        update = {
            set(text) { menuItem.text = it }
            set(selected) { menuItem.isSelected = it }
            set(enabled) { menuItem.isEnabled = it }
            set(onSelect) { 
                menuItem.actionListeners.forEach { menuItem.removeActionListener(it) }
                menuItem.addActionListener { if (menuItem.isSelected) onSelect() }
            }
        }
    )
}

/**
 * A composable wrapper for JSeparator in menus.
 */
@Composable
fun MenuSeparator() {
    ComposeNode<Any, androidx.compose.runtime.Applier<Any>>(
        factory = { JSeparator() },
        update = {}
    )
}
