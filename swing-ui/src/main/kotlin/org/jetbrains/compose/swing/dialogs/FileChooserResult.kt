package org.jetbrains.compose.swing.dialogs

import java.io.File

/**
 * The outcome of a file-chooser dialog.
 */
public sealed interface FileChooserResult {
    /**
     * The user approved a selection. [files] holds the chosen files in selection order and is never
     * empty; [file] is the first of them, the only entry when multi-selection is disabled.
     */
    public class Approved(
        public val files: List<File>,
    ) : FileChooserResult {
        /** The first chosen file. */
        public val file: File
            get() = files.first()
    }

    /** The user dismissed the dialog without approving a selection. */
    public object Cancelled : FileChooserResult
}
