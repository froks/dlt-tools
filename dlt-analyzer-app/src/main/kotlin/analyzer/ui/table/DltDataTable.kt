package analyzer.ui.table

import analyzer.filter.DltMessageFilter
import analyzer.plugin.DltTargetAware
import db.DltTarget
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class DltDataTable : JScrollPane(JTable(), VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED), DltTargetAware {
    private var dltTarget: DltTarget? = null

    private val table: JTable
        get() = this.viewport.view as JTable

    private val dltTableModel: DltTableModel?
        get() = this.table.model as? DltTableModel

    private var dltFilterList: List<DltMessageFilter>? = null

    init {
        val menu = JPopupMenu()
        val copy = JMenuItem("Copy message")
        menu.add(copy)
        copy.addActionListener {
            val sb = StringBuilder()
            if (table.selectedRowCount > 10_000) {
                throw RuntimeException("Too many rows selected")
            }
            table.selectedRows.forEach {
                val value = (table.model.getValueAt(it, TableColumns.MESSAGE.ordinal) as String)
                sb.append(value)
                if (!value.endsWith("\n")) {
                    sb.append("\n")
                }
            }
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(sb.toString()), null)
        }
        table.componentPopupMenu = menu
        val renderer = DltStringRenderer()
        table.setDefaultRenderer(String::class.java, renderer)
        table.setDefaultRenderer(Long::class.java, renderer)
    }

    override fun setDltTarget(dltTarget: DltTarget?) {
        if (this.dltTarget != dltTarget) {
            val model = if (dltTarget != null) {
                DltTableModel(dltTarget, dltFilterList)
            } else {
                DefaultTableModel()
            }
            table.model = model

            if (model.columnCount > 0) {
                table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
                val columnModel = table.columnModel
                TableColumns.entries.forEachIndexed { index, tableColumns ->
                    if (tableColumns.preferredWidth != -1) {
                        val column = columnModel.getColumn(index)
                        column.preferredWidth = tableColumns.preferredWidth
                        column.width = tableColumns.preferredWidth
                        column.maxWidth = tableColumns.preferredWidth
                    }
                }
            }
            this.dltTarget = dltTarget
            table.updateUI()
        }
    }

    fun updateFilters(filterList: List<DltMessageFilter>) {
        dltFilterList = filterList
        dltTableModel?.filterList = filterList
    }
}
