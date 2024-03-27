package analyzer.ui.table

import db.DltTarget
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTable

class DltDataTable(dltTarget: DltTarget) : JTable(createModel(dltTarget)) {
    init {
        this.setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN)
        val columnModel = this.getColumnModel()
        TableColumns.entries.forEachIndexed { index, tableColumns ->
            if (tableColumns.preferredWidth != -1) {
                val column = columnModel.getColumn(index)
                column.preferredWidth = tableColumns.preferredWidth
                column.width = tableColumns.preferredWidth
                column.maxWidth = tableColumns.preferredWidth
            }
        }
        val menu = JPopupMenu()
        val copy = JMenuItem("Copy message")
        menu.add(copy)
        copy.addActionListener {
            val sb = StringBuilder()
            if (this.selectedRowCount > 10_000) {
                throw RuntimeException("Too many rows selected")
            }
            this.selectedRows.forEach {
                val value = (model.getValueAt(it, TableColumns.MESSAGE.ordinal) as String)
                sb.append(value)
                if (!value.endsWith("\n")) {
                    sb.append("\n")
                }
            }
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(sb.toString()), null)
        }

        componentPopupMenu = menu
    }

    fun setDltTarget(dltTarget: DltTarget) {
        (model as DltTableModel).setDltTarget(dltTarget)
    }

    companion object {
        fun createModel(dltTarget: DltTarget): DltTableModel =
            DltTableModel(dltTarget)
    }
}
