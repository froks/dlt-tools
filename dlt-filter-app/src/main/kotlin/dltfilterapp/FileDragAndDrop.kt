package dltfilterapp

import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.TransferHandler

class FileDropHandler(private val callback: (List<File>) -> Unit) : TransferHandler() {
    override fun canImport(support: TransferSupport): Boolean {
        return support.dataFlavors.any { it.isFlavorJavaFileListType }
    }

    override fun importData(support: TransferSupport): Boolean {
        if (!canImport(support)) {
            return false
        }
        try {
            val files = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
            callback(files)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}