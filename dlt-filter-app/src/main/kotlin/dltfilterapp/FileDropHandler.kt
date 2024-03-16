package dltfilterapp

import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.io.File
import javax.swing.TransferHandler

class FileDropHandler(private val processFile: (File) -> Boolean) : TransferHandler() {
    override fun importData(support: TransferSupport): Boolean {
        val transferData = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
        support.dropAction = DnDConstants.ACTION_COPY
        return processFile(transferData.first())
    }

    override fun getDragImage(): Image? =
        null

    override fun canImport(support: TransferSupport): Boolean {
        val transferData = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
        support.setShowDropLocation(true)
        support.dropAction = DnDConstants.ACTION_COPY
        return transferData.size == 1 && transferData.all { it.absolutePath.endsWith(".dlt") }
    }
}
