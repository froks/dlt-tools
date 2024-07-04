package dltfilterapp

import java.awt.FileDialog
import java.awt.FileDialog.LOAD
import java.awt.FileDialog.SAVE
import java.awt.Frame
import java.io.File

class DltFileChooser {
    companion object {
        fun showDialog(parent: Frame?, title: String, currentDirectory: File?, name: String? = null, save: Boolean = false): List<File> {
            val d = FileDialog(parent, title, if (save) SAVE else LOAD)
            d.isMultipleMode = !save
            d.setFilenameFilter { _, fileName ->
                fileName.endsWith(".dlt", ignoreCase = true)
            }
            d.directory = currentDirectory?.absolutePath
            if (name != null) {
                d.file = name
            } else {
                d.file = "*.dlt"
            }
            d.isVisible = true
            val files = d.files.asList()
            return files.filter { it.name.endsWith(".dlt", ignoreCase = true) };
        }
    }
}
