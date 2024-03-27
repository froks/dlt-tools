package analyzer.ui.table

import analyzer.DefaultDateTimeFormatter
import analyzer.formatDefault
import db.DltMessageDto
import db.DltTableDataAccess
import db.DltTarget
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.event.TableModelListener
import javax.swing.table.TableModel
import kotlin.reflect.KClass

enum class TableColumns(val title: String, val columnClass: KClass<*>, val preferredWidth: Int) {
    ID("ID", Long::class, 60),
    APPID("AppId", String::class, 50),
    TIMESTAMP("Timestamp", String::class, 140),
    LOG_INFO("Type", String::class, 60),
    MESSAGE("Message", String::class, -1),
}


class DltTableModel(private var dltTarget: DltTarget) : TableModel {
    private val CACHED_ENTRIES_COUNT = 1_000

    private var tableAccess: DltTableDataAccess
    private var rowCount: Int = 0

    private var listOffset = 0
    private var cachedEntries: List<DltMessageDto> = emptyList()

    init {
        tableAccess = DltTableDataAccess(dltTarget.dataSource)
        rowCount = tableAccess.getEntryCount().toInt()
    }

    fun setDltTarget(dltTarget: DltTarget) {
        if (this.dltTarget != dltTarget) {
            this.dltTarget = dltTarget
            this.tableAccess = DltTableDataAccess(dltTarget.dataSource)
        }
    }

    override fun getRowCount(): Int =
        rowCount

    override fun getColumnCount(): Int =
        TableColumns.entries.size

    override fun getColumnName(columnIndex: Int): String =
        TableColumns.entries[columnIndex].title

    override fun getColumnClass(columnIndex: Int): Class<*> =
        TableColumns.entries[columnIndex].columnClass.java

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean =
        false

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val isAvailable = rowIndex in listOffset until listOffset + cachedEntries.size
        if (!isAvailable) {
            val offset = (rowIndex - (CACHED_ENTRIES_COUNT / 2)).coerceAtLeast(0)

            cachedEntries = tableAccess.readData("1=1 ORDER BY id LIMIT $CACHED_ENTRIES_COUNT OFFSET $offset")
            listOffset = offset
        }

        val entry = cachedEntries[rowIndex - listOffset]

        return when (TableColumns.entries[columnIndex]) {
            TableColumns.ID -> entry.id
            TableColumns.APPID -> entry.appId
            TableColumns.TIMESTAMP -> entry.timestamp.formatDefault()
            TableColumns.LOG_INFO -> entry.messageType.shortText
            TableColumns.MESSAGE -> entry.message
        }
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
    }

    override fun addTableModelListener(l: TableModelListener) {
    }

    override fun removeTableModelListener(l: TableModelListener) {
    }
}
