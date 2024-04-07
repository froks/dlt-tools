package analyzer.ui.helper

import java.awt.Component
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class DltFileChooser(private val title: String, private val currentDirectory: File?, private val name: String? = null) {
    fun showDialog(parent: Component?): File? {
        val chooser = JFileChooser(currentDirectory)
        chooser.dialogTitle = title
        val filter = FileNameExtensionFilter("DLT log files (*.dlt)", "dlt")
        chooser.fileFilter = filter
        chooser.currentDirectory = currentDirectory
        if (currentDirectory != null && name != null) {
            chooser.approveButtonText = "Save"
            chooser.selectedFile = File(currentDirectory, name)
        }
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return chooser.selectedFile
        }
        return null
    }
}
