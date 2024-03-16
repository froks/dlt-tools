package dltfilterapp

import java.awt.Component
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class DltFileChooser(title: String, currentDirectory: File?, name: String? = null) : JFileChooser(currentDirectory) {
    init {
        this.dialogTitle = title
        val filter = FileNameExtensionFilter("DLT log files (*.dlt)", "dlt")
        this.fileFilter = filter
        this.currentDirectory = currentDirectory
        if (currentDirectory != null && name != null) {
            this.approveButtonText = "Save"
            this.selectedFile = File(currentDirectory, name)
        }
    }

    fun showDialog(parent: Component?): File? {
        if (this.showOpenDialog(parent) == APPROVE_OPTION) {
            return this.selectedFile
        }
        return null;
    }
}
