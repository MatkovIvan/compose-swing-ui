package org.jetbrains.compose.swing.components.layout

import androidx.compose.runtime.Composable
import javax.swing.Icon

/**
 * Declarative tabs of a [TabbedPane]. Each [tab] call appends one tab, in call order.
 */
public interface TabbedPaneScope {
    /**
     * Declares one tab.
     *
     * The tab is given an identity scoped to a single [TabbedPane] composition instance: it keeps the
     * tab stable across recomposition and reordering of the surrounding declarations, but it does not
     * persist if the tab is unmounted and later remounted.
     *
     * @param title the tab's title
     * @param icon the tab's icon, or `null` for none
     * @param tooltip the tab's tooltip, or `null` for none
     * @param enabled whether the tab can be selected
     * @param content the composable shown in the tab's body when it is selected
     */
    public fun tab(
        title: String,
        icon: Icon? = null,
        tooltip: String? = null,
        enabled: Boolean = true,
        content: @Composable () -> Unit,
    )
}
