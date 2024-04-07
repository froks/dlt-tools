package analyzer.ui.filter

import analyzer.filter.DltMessageFilter
import javax.swing.table.AbstractTableModel

class FilterListTableModel(private var listArg: MutableList<DltMessageFilter>) : AbstractTableModel() {
    var list: MutableList<DltMessageFilter>
        get() =
            listArg
        set(v) {
            listArg = v
            fireTableDataChanged()
        }

    override fun getRowCount(): Int =
        list.size

    override fun getColumnCount(): Int =
        2

    override fun getColumnName(columnIndex: Int): String =
        when (columnIndex) {
            0 -> ""
            else -> "Description"
        }

    override fun getColumnClass(columnIndex: Int): Class<*> =
        when (columnIndex) {
            0 -> Boolean::class.javaObjectType
            else -> String::class.java
        }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean =
        when (columnIndex) {
            0 -> true
            else -> false
        }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = list[rowIndex]
        return when (columnIndex) {
            0 -> entry.active
            else -> entry.filterName
        }
    }

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        val entry = list[rowIndex]
        when (columnIndex) {
            0 -> entry.active = value as Boolean
        }
        fireTableRowsUpdated(rowIndex, rowIndex)
    }

    fun addEntry(entry: DltMessageFilter) {
        list.add(entry)
        fireTableRowsInserted(list.size - 1, list.size - 1)
    }

    fun replaceEntry(index: Int, entry: DltMessageFilter) {
        list[index] = entry
        fireTableRowsUpdated(index, index)
    }

    fun removeEntries(indices: IntArray) {
        for (i in indices.sortedDescending()) {
            list.removeAt(i)
            fireTableRowsDeleted(i, i)
        }
    }
}
