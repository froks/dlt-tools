package analyzer.ui.sxs

import analyzer.asIcon
import analyzer.formatDefault
import analyzer.plugin.DltTargetAware
import db.DltTableDataAccess
import db.DltTarget
import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import javax.swing.*
import kotlin.concurrent.thread

class LogPanel : JPanel(BorderLayout()), DltTargetAware {
    private val logger = LoggerFactory.getLogger(LogPanel::class.java)

    private var appId: String = ""
    private var textArea: JTextArea
    private var textField: JTextField
    private var dltTarget: DltTarget? = null

    init {
        textField = JTextField("").also { textField ->
            textField.putClientProperty("JTextField.leadingIcon", GoogleMaterialDesignIcons.SEARCH.asIcon(16f))
            textField.putClientProperty("JTextField.placeholderText", "Enter App-ID and press enter")
            textField.addActionListener { _ ->
                updateTextArea(textField.text.trim())
            }
        }

        textArea = JTextArea("").also {
            it.isEditable = false
//            it.background = Color.WHITE
        }
        val textAreaScroll = JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)

        this.add(textField, BorderLayout.NORTH)
        this.add(textAreaScroll, BorderLayout.CENTER)

    }

    private fun updateTextArea(appId: String, force: Boolean = false) {
        logger.info("Updating Text")
        if (this.appId != appId || force) {
            thread(isDaemon = true) {
                if (dltTarget == null) {
                    SwingUtilities.invokeLater {
                        textArea.text = "No dlt target is selected"
                    }
                    return@thread
                }
                textArea.text = "Loading data"
                val dao = DltTableDataAccess(dltTarget!!.dataSource)
                val appIds = appId.split(',', ' ', ';').filter { it.isNotBlank() }
                val data = dao.readDataPrepared("app_id IN (${appIds.joinToString(",") { "?" }}) ORDER BY id") {
                    appIds.forEachIndexed { index, s ->
                        it.setString(index + 1, s)
                    }
                }
                val text = data.joinToString("\n") {
                    if (appIds.size > 1) {
                        "[${it.timestamp.formatDefault()} ${it.appId.padEnd(4, ' ')} ${it.messageType.shortText.padEnd(5, ' ')}] ${it.message.trimEnd()}"
                    } else {
                        "[${it.timestamp.formatDefault()} ${it.messageType.shortText.padEnd(5, ' ')}] ${it.message.trimEnd()}"
                    }
                }
                SwingUtilities.invokeLater {
                    textArea.text = text
                    textArea.caretPosition = 0
                }
            }
        }
        this.appId = appId
    }

    override fun setDltTarget(dltTarget: DltTarget?) {
        this.dltTarget = dltTarget
        updateTextArea(this.appId, true)
    }
}
