package analyzer.ui.table

import analyzer.filter.DltMessageFilter
import analyzer.filter.sqlWhere
import analyzer.formatDefault
import db.DltMessageDto
import db.DltTableDataAccess
import db.DltTarget
import org.slf4j.LoggerFactory
import java.awt.Color
import javax.swing.table.AbstractTableModel
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

enum class TableColumns(val title: String, val columnClass: KClass<*>, val preferredWidth: Int) {
    ID("ID", Long::class, 60),
    APPID("AppId", String::class, 50),
    TIMESTAMP("Timestamp", String::class, 140),
    LOG_INFO("Type", String::class, 60),
    MESSAGE("Message", String::class, -1),
}


class DltTableModel(private var dltTarget: DltTarget, private var internalFilterList: List<DltMessageFilter>?) : AbstractTableModel() {
    private val log = LoggerFactory.getLogger(DltTableModel::class.java)

    var filterList: List<DltMessageFilter>?
        get() = internalFilterList
        set(v) {
            internalFilterList = v
            rowCount = tableAccess.getEntryCount(v.sqlWhere()).toInt()
            cachedEntries = emptyList()
            cachedEntriesColor = mutableMapOf()
            fireTableDataChanged()
        }

    private val CACHED_ENTRIES_COUNT = 1_000

    private var tableAccess: DltTableDataAccess
    private var rowCount: Int = 0

    private var listOffset = 0
    private var cachedEntries: List<DltMessageDto> = emptyList()
    private var cachedEntriesColor: MutableMap<Int, Color> = mutableMapOf()

    init {
        tableAccess = DltTableDataAccess(dltTarget.dataSource)
        rowCount = tableAccess.getEntryCount(filterList.sqlWhere()).toInt()
    }

    fun setDltTarget(dltTarget: DltTarget) {
        if (this.dltTarget != dltTarget) {
            this.dltTarget = dltTarget
            this.tableAccess = DltTableDataAccess(dltTarget.dataSource)
            fireTableDataChanged()
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

    private fun getRow(rowIndex: Int): DltMessageDto {
        val isAvailable = rowIndex in listOffset until listOffset + cachedEntries.size
        if (!isAvailable) {
            val offset = (rowIndex - (CACHED_ENTRIES_COUNT / 2)).coerceAtLeast(0)

            val duration = measureTimeMillis {
                cachedEntries = tableAccess.readData(filterList.sqlWhere(), offset, CACHED_ENTRIES_COUNT)
            }
            log.info("Reading ${cachedEntries.size} entries took $duration ms")
            listOffset = offset
        }

        return cachedEntries[rowIndex - listOffset]
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = getRow(rowIndex)

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

    fun getRowColor(rowIndex: Int): Color? {
        if (cachedEntriesColor.containsKey(rowIndex)) {
            return cachedEntriesColor[rowIndex]
        }

        val color = cachedEntriesColor[rowIndex]
        if (color == null) {
            val entry = getRow(rowIndex)
            val match = filterList?.firstOrNull { it.active && it.markerActive && it.textColor != null && it.matches(entry) }
            if (match != null) {
                cachedEntriesColor[rowIndex] = match.textColor!!
                return match.textColor!!
            }
        }
        return null
    }
}
