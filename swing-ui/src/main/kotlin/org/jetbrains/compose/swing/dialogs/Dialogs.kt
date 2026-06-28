package org.jetbrains.compose.swing.dialogs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.jetbrains.compose.swing.constants.ConfirmOption
import org.jetbrains.compose.swing.constants.ConfirmResult
import org.jetbrains.compose.swing.constants.FileSelectionMode
import org.jetbrains.compose.swing.constants.MessageType
import java.awt.Color
import java.awt.Component
import java.io.File
import javax.swing.JColorChooser
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.UIManager

/**
 * Shows a modal message dialog and suspends until the user dismisses it.
 *
 * Invoke from a coroutine launched in composition (for example inside a `LaunchedEffect`) or from an
 * event handler. The calling coroutine resumes once the dialog closes.
 *
 * @param message the message to display.
 * @param parent the component over which the dialog is centered, or `null` to center on screen.
 * @param title the dialog window title, or `null` to use the platform default.
 * @param messageType the kind of message, which selects the icon shown (a [MessageType] `JOptionPane`
 *   message type).
 */
public suspend fun showMessageDialog(
    message: String,
    parent: Component? = null,
    title: String? = null,
    @MessageType messageType: Int = JOptionPane.INFORMATION_MESSAGE,
) {
    withContext(Dispatchers.Swing) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            title ?: messageDialogTitle(),
            messageType,
        )
    }
}

/**
 * Shows a modal confirm dialog and suspends until the user answers it.
 *
 * Invoke from a coroutine launched in composition (for example inside a `LaunchedEffect`) or from an
 * event handler.
 *
 * @param message the message to display.
 * @param parent the component over which the dialog is centered, or `null` to center on screen.
 * @param title the dialog window title, or `null` to use the platform default.
 * @param option the set of buttons to offer (a [ConfirmOption] `JOptionPane` option type).
 * @param messageType the kind of message, which selects the icon shown (a [MessageType] `JOptionPane`
 *   message type).
 * @return which button the user chose as a [ConfirmResult] `JOptionPane` return code
 *   (`JOptionPane.YES_OPTION`, `NO_OPTION`, `OK_OPTION`, `CANCEL_OPTION`), or `JOptionPane.CLOSED_OPTION`
 *   when the dialog was dismissed. `YES_OPTION` and `OK_OPTION` share the value `0`; interpret the
 *   return against the [option] passed.
 */
@ConfirmResult
public suspend fun showConfirmDialog(
    message: String,
    parent: Component? = null,
    title: String? = null,
    @ConfirmOption option: Int = JOptionPane.YES_NO_OPTION,
    @MessageType messageType: Int = JOptionPane.QUESTION_MESSAGE,
): Int =
    withContext(Dispatchers.Swing) {
        JOptionPane.showConfirmDialog(
            parent,
            message,
            title ?: optionDialogTitle(),
            option,
            messageType,
        )
    }

/**
 * Shows a modal text-input dialog and suspends until the user dismisses it.
 *
 * Invoke from a coroutine launched in composition (for example inside a `LaunchedEffect`) or from an
 * event handler.
 *
 * @param message the prompt to display.
 * @param parent the component over which the dialog is centered, or `null` to center on screen.
 * @param title the dialog window title, or `null` to use the platform default.
 * @param initialValue the text the input field starts with.
 * @param messageType the kind of message, which selects the icon shown (a [MessageType] `JOptionPane`
 *   message type).
 * @return the entered text, or `null` if the user cancelled the dialog.
 */
public suspend fun showInputDialog(
    message: String,
    parent: Component? = null,
    title: String? = null,
    initialValue: String = "",
    @MessageType messageType: Int = JOptionPane.QUESTION_MESSAGE,
): String? =
    withContext(Dispatchers.Swing) {
        JOptionPane.showInputDialog(
            parent,
            message,
            title ?: optionDialogTitle(),
            messageType,
            null,
            null,
            initialValue,
        ) as String?
    }

/**
 * Shows a modal open-file dialog and suspends until the user dismisses it.
 *
 * Invoke from a coroutine launched in composition (for example inside a `LaunchedEffect`) or from an
 * event handler.
 *
 * @param parent the component over which the dialog is centered, or `null` to center on screen.
 * @param title the dialog window title, or `null` to use the platform default.
 * @param selectionMode what the user is allowed to select (a [FileSelectionMode] `JFileChooser` mode).
 * @param multiSelection whether the user may select more than one entry.
 * @return [FileChooserResult.Approved] with the chosen files, or [FileChooserResult.Cancelled].
 */
public suspend fun showOpenDialog(
    parent: Component? = null,
    title: String? = null,
    @FileSelectionMode selectionMode: Int = JFileChooser.FILES_ONLY,
    multiSelection: Boolean = false,
): FileChooserResult =
    withContext(Dispatchers.Swing) {
        val chooser = newFileChooser(title, selectionMode, multiSelection)
        val returnValue = chooser.showOpenDialog(parent)
        fileChooserResult(returnValue, chooser.selectedFilesOrEmpty(multiSelection))
    }

/**
 * Shows a modal save-file dialog and suspends until the user dismisses it.
 *
 * Invoke from a coroutine launched in composition (for example inside a `LaunchedEffect`) or from an
 * event handler.
 *
 * @param parent the component over which the dialog is centered, or `null` to center on screen.
 * @param title the dialog window title, or `null` to use the platform default.
 * @param selectionMode what the user is allowed to select (a [FileSelectionMode] `JFileChooser` mode).
 * @return [FileChooserResult.Approved] with the chosen file, or [FileChooserResult.Cancelled].
 */
public suspend fun showSaveDialog(
    parent: Component? = null,
    title: String? = null,
    @FileSelectionMode selectionMode: Int = JFileChooser.FILES_ONLY,
): FileChooserResult =
    withContext(Dispatchers.Swing) {
        val chooser = newFileChooser(title, selectionMode, multiSelection = false)
        val returnValue = chooser.showSaveDialog(parent)
        fileChooserResult(returnValue, chooser.selectedFilesOrEmpty(multiSelection = false))
    }

/**
 * Shows a modal color-chooser dialog and suspends until the user dismisses it.
 *
 * Invoke from a coroutine launched in composition (for example inside a `LaunchedEffect`) or from an
 * event handler.
 *
 * @param parent the component over which the dialog is centered, or `null` to center on screen.
 * @param title the dialog window title, or `null` to use the platform default.
 * @param initialColor the color selected when the dialog opens, or `null` to start from white.
 * @return the chosen color, or `null` if the user cancelled the dialog.
 */
public suspend fun showColorDialog(
    parent: Component? = null,
    title: String? = null,
    initialColor: Color? = null,
): Color? =
    withContext(Dispatchers.Swing) {
        JColorChooser.showDialog(parent, title, initialColor ?: Color.WHITE)
    }

private fun newFileChooser(
    title: String?,
    @FileSelectionMode selectionMode: Int,
    multiSelection: Boolean,
): JFileChooser =
    JFileChooser().apply {
        if (title != null) dialogTitle = title
        fileSelectionMode = selectionMode
        isMultiSelectionEnabled = multiSelection
    }

/**
 * Maps the integer a `JFileChooser` returns, together with the [selectedFiles] it would expose on
 * approval, to a [FileChooserResult]. Any value other than `JFileChooser.APPROVE_OPTION` (a cancel or
 * an error), or an approval with no usable selection, maps to [FileChooserResult.Cancelled] so
 * [FileChooserResult.Approved.files] is never empty.
 */
private fun fileChooserResult(
    returnValue: Int,
    selectedFiles: List<File>,
): FileChooserResult =
    if (returnValue == JFileChooser.APPROVE_OPTION && selectedFiles.isNotEmpty()) {
        FileChooserResult.Approved(selectedFiles)
    } else {
        FileChooserResult.Cancelled
    }

private fun JFileChooser.selectedFilesOrEmpty(multiSelection: Boolean): List<File> =
    if (multiSelection) {
        selectedFiles.toList()
    } else {
        selectedFile?.let { listOf(it) }.orEmpty()
    }

private fun messageDialogTitle(): String = UIManager.getString("OptionPane.messageDialogTitle") ?: "Message"

private fun optionDialogTitle(): String = UIManager.getString("OptionPane.titleText") ?: "Select an Option"
