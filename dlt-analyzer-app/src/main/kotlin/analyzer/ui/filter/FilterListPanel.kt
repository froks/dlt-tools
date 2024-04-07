package analyzer.ui.filter

import analyzer.asIcon
import analyzer.filter.DltMessageFilter
import analyzer.ui.helper.findParentFrame
import com.formdev.flatlaf.FlatClientProperties
import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.prefs.Preferences
import javax.swing.*

class FilterListPanel(private val savePrefix: String) : JPanel(BorderLayout()) {
    val filterList: List<DltMessageFilter>
        get() = model.list

    var filtersChanged: () -> Unit = {}

    private val searchField = JTextField()
    private val model = FilterListTableModel(mutableListOf()).also { model ->
        model.addTableModelListener {
            savePrefs()
            filtersChanged.invoke()
        }
    }

    private val table = JTable(model).also { tbl ->
        tbl.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        tbl.cellSelectionEnabled = false
        tbl.rowSelectionAllowed = true
        tbl.tableHeader.columnModel.getColumn(0).maxWidth = 20
        tbl.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                val col = tbl.columnAtPoint(e.point)
                if (e.clickCount == 2 && col != 0) {
                    val entry = model.list[tbl.selectedRow]
                    val newEntry = FilterEditDialog.editFilter(tbl.findParentFrame(), entry)
                    if (newEntry != null) {
                        model.replaceEntry(tbl.selectedRow, newEntry)
                        savePrefs()
                        filtersChanged.invoke()
                    }
                }
            }

            override fun mousePressed(e: MouseEvent) {
            }

            override fun mouseReleased(e: MouseEvent) {
            }

            override fun mouseEntered(e: MouseEvent) {
            }

            override fun mouseExited(e: MouseEvent) {
            }
        })
    }

    init {
        val controlBar = JToolBar()

        val addButton = JButton()
        addButton.icon = GoogleMaterialDesignIcons.ADD.asIcon(16f)
        addButton.toolTipText = "Add new entry"
        addButton.addActionListener {
            val entry = FilterEditDialog.newFilter(this.findParentFrame())
            if (entry != null) {
                model.addEntry(entry)
                savePrefs()
                filtersChanged.invoke()
            }
        }
        controlBar.add(addButton)

        val deleteButton = JButton()
        deleteButton.icon = GoogleMaterialDesignIcons.DELETE.asIcon(16f)
        deleteButton.toolTipText = "Delete selected entries"
        deleteButton.addActionListener {
            val selected = table.selectedRows
            if (selected.isEmpty()) {
                return@addActionListener
            }
            if (JOptionPane.showConfirmDialog(this, "Really delete ${selected.size} entries?") == JOptionPane.YES_OPTION) {
                model.removeEntries(selected)
                savePrefs()
                filtersChanged.invoke()
            }

        }
        controlBar.add(deleteButton)

        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search for tags and names")
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, GoogleMaterialDesignIcons.SEARCH.asIcon(16f))
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, controlBar)
        add(searchField, BorderLayout.NORTH)
        add(JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER)

        loadPrefs()
        filtersChanged.invoke()
    }

    private fun loadPrefs() {
        val json = Preferences.userRoot().node("dlt-analyzer").node("filter").get(savePrefix, null)
        if (json != null) {
            val obj = Json.decodeFromString<FilterPreferences>(json)
            model.list = obj.filterList.toMutableList()
        }
    }

    private fun savePrefs() {
        val prefs = FilterPreferences(model.list)
        Preferences.userRoot().node("dlt-analyzer").node("filter").put(savePrefix, Json.encodeToString(prefs))
    }
}

@Serializable
data class FilterPreferences(
    val filterList: List<DltMessageFilter>
)

