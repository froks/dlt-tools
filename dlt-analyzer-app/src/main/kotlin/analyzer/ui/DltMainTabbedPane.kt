package analyzer.ui

import analyzer.asIcon
import analyzer.plugin.DltTargetAware
import analyzer.plugin.TabActivation
import analyzer.ui.sxs.LogPanel
import analyzer.ui.table.DltTablePane
import com.formdev.flatlaf.FlatClientProperties
import db.DltTarget
import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons
import javax.swing.JTabbedPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class DltMainTabbedPane : JTabbedPane(LEFT) {
    private var dltTarget: DltTarget? = null

    init {
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_ICON_PLACEMENT, SwingConstants.TOP)

        addTab("Table", GoogleMaterialDesignIcons.GRID_ON.asIcon(), DltTablePane())
        addTab("Text", GoogleMaterialDesignIcons.DESCRIPTION.asIcon(), LogPanel())

        addChangeListener { _ ->
            updateComponents()
        }
    }

    private fun updateComponents() {
        val c = this.selectedComponent
        if (c is DltTargetAware) {
            SwingUtilities.invokeLater {
                c.setDltTarget(dltTarget)
            }
        }
        if (c is TabActivation) {
            c.tabActivated()
        }
    }

    fun setDltTarget(dltTarget: DltTarget) {
        this.dltTarget = dltTarget
        updateComponents()
    }
}
