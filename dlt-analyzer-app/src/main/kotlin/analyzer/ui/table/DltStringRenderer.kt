package analyzer.ui.table

import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class DltStringRenderer : DefaultTableCellRenderer() {
    private val defaultColor = this.foreground

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val model = table.model as DltTableModel
        val color = model.getRowColor(row)
        if (color != null) {
            foreground = color
        } else {
            foreground = defaultColor
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    }
}
