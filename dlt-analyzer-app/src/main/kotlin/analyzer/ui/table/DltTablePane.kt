package analyzer.ui.table

import analyzer.plugin.DltTargetAware
import analyzer.ui.filter.FilterListPanel
import db.DltTarget
import javax.swing.JSplitPane

class DltTablePane : JSplitPane(HORIZONTAL_SPLIT), DltTargetAware {
    private val table = DltDataTable()
    private val filter = FilterListPanel("table").also { filter ->
        filter.filtersChanged = { filtersChanged() }
    }

    init {
        add(filter, LEFT)
        add(table, RIGHT)
    }

    private fun filtersChanged() {
        table.updateFilters(filter.filterList)
    }

    override fun setDltTarget(dltTarget: DltTarget?) {
        table.updateFilters(filter.filterList)
        table.setDltTarget(dltTarget)
    }
}
