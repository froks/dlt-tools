package dltfilterapp

import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons
import jiconfont.swing.IconFontSwing
import dltcore.DltMessageParser
import dltcore.DltMessageV1
import dltcore.asIntValue
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.prefs.Preferences
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class DltFilterApp : JFrame("dlt-filter") {
    private val dltTools = Preferences.userRoot().node("dlt-tools")
    private lateinit var btnOpenFile: JButton
    private lateinit var progressBar: JProgressBar
    private lateinit var textField: JTextField

    init {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            JOptionPane.showMessageDialog(
                this, "${e.javaClass.simpleName}: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE
            )
        }

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        IconFontSwing.register(GoogleMaterialDesignIcons.getIconFont())
        this.iconImage = IconFontSwing.buildImage(GoogleMaterialDesignIcons.TRANSFORM, 20f, Color.GREEN)
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                saveWindowPreferences()
            }

            override fun componentMoved(e: ComponentEvent) {
                saveWindowPreferences()
            }
        })

        addComponents()
    }

    private fun addComponents() {
        this.contentPane.add(createEditorPanel(), BorderLayout.NORTH)
        this.contentPane.add(createProcessFilePanel(), BorderLayout.CENTER)
        this.contentPane.add(createProgressBar(), BorderLayout.SOUTH)
    }

    private fun createEditorPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.add(this.createEditor(), BorderLayout.NORTH)
        return panel
    }

    private fun createProcessFilePanel(): JPanel {
        val panel = JPanel()
        panel.add(createProcessFileButton())
        return panel
    }

    private fun createEditor(): JPanel {
        val panel = JPanel()
        val layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.layout = layout
        panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        panel.add(createTextField())

        return panel
    }

    private fun createTextField(): JTextField {
        textField = JTextField()
        textField.isEditable = true
        textField.setUI(HintTextFieldUI("enter appids separated by space", false))
        textField.toolTipText = "enter appids separated by space"
        textField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                saveLastText()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                saveLastText()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                saveLastText()
            }
        })
        return textField
    }

    private fun createProgressBar(): JProgressBar {
        progressBar = JProgressBar()
        progressBar.isStringPainted = true
        progressBar.string = "set appids to filter and click process file"
        progressBar.maximum = 100
        progressBar.value = 0
        return progressBar
    }

    private fun createProcessFileButton(): JButton {
        val folderOpenIcon = IconFontSwing.buildIcon(GoogleMaterialDesignIcons.FOLDER_OPEN, 40f, Color.BLUE)

        btnOpenFile = JButton(folderOpenIcon)
        btnOpenFile.isFocusable = false
        btnOpenFile.text = "Process file"
        btnOpenFile.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        btnOpenFile.addActionListener {
            SwingUtilities.invokeLater {
                val lastDirectory = dltTools.get("lastDirectory", null)?.toExistingDirectory()
                val fileChooser = DltFileChooser("Select file to process", lastDirectory)
                val file = fileChooser.showDialog(null)
                if (file != null) {
                    dltTools.put("lastDirectory", file.parent)
                    processFile(file)
                }
            }
        }

        return btnOpenFile
    }

    private fun loadPreferences() {
        loadWindowPreferences()
        SwingUtilities.invokeLater {
            textField.text = dltTools.get("lastFilterText", "")

            textField.requestFocus()
            textField.caretPosition = textField.text.length
        }
    }

    private fun loadWindowPreferences() {
        val x = dltTools.get("window.x", "0").toInt()
        val y = dltTools.get("window.y", "0").toInt()
        val width = dltTools.get("window.w", "800").toInt()
        val height = dltTools.get("window.h", "600").toInt()

        setBounds(x, y, width, height)
    }

    private var windowPrefsTimer: Timer? = null
    private fun saveWindowPreferences() {
        SwingUtilities.invokeLater {
            synchronized(this) {
                if (windowPrefsTimer == null) {
                    windowPrefsTimer = Timer(400) {
                        windowPrefsTimer!!.stop()
                        dltTools.put("window.x", x.toString())
                        dltTools.put("window.y", y.toString())
                        dltTools.put("window.w", width.toString())
                        dltTools.put("window.h", height.toString())
                    }
                } else {
                    windowPrefsTimer!!.restart()
                }
            }
        }
    }

    private var lastTextTimer: Timer? = null
    private fun saveLastText() {
        SwingUtilities.invokeLater {
            synchronized(this) {
                if (!textField.text.equals(dltTools.get("lastFilterText", null))) {
                    return@invokeLater
                }
                if (lastTextTimer == null) {
                    lastTextTimer = Timer(1000) {
                        lastTextTimer!!.stop()
                        dltTools.put("lastFilterText", textField.text)
                    }
                } else {
                    lastTextTimer!!.restart()
                }
            }
        }
    }

    private fun processFile(file: File): Boolean {
        val appIds = textField.text.verifyAndFormatAppIds()
        if (appIds == null) {
            JOptionPane.showMessageDialog(
                this,
                "No appids are entered, aborting",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
            return false
        }
        val appIdsFiltered = appIds.split(';').map { it.asIntValue() }.toSet()

        val destinationFile = DltFileChooser(
            "Choose destination file", File(file.parent), file.nameWithoutExtension + "-filtered" + ".dlt"
        ).showDialog(this) ?: return false
        if (destinationFile.exists()) {
            val res = JOptionPane.showConfirmDialog(
                this,
                "File exists already. Overwrite?",
                "Confirmation",
                JOptionPane.YES_OPTION or JOptionPane.CANCEL_OPTION
            )
            if (res == JOptionPane.CANCEL_OPTION) {
                return false
            }
            if (!destinationFile.delete()) {
                throw IOException("Couldn't delete file ${destinationFile.absolutePath}")
            }
        }

        thread {
            SwingUtilities.invokeAndWait {
                progressBar.isStringPainted = true
                progressBar.string = "processing"
                progressBar.maximum = 100
                progressBar.value = 0
            }

            var percent = 0f
            var lastPercent = 0f

            RandomAccessFile(destinationFile, "rw").use { randomAccessFile ->

                val bb = ByteBuffer.allocate(100_000)
                DltMessageParser.parseFileWithCallback(file.toPath()) { message, status ->
                    if (status.filePosition != null && status.fileSize != null) {
                        percent = (status.filePosition!!.toFloat() / status.fileSize!!.toFloat()) * 100
                    } else if (percent == 0f) {
                        SwingUtilities.invokeLater {
                            progressBar.isIndeterminate = true
                        }
                    }

                    if (percent > lastPercent + 1) {
                        lastPercent = percent
                        SwingUtilities.invokeLater {
                            if (!progressBar.isIndeterminate) {
                                progressBar.value = percent.roundToInt()
                            }
                        }
                    }


                    if (message is DltMessageV1) {
                        if (!appIdsFiltered.contains(message.extendedHeader?.apid)) {
                            return@parseFileWithCallback
                        }
                    }

                    message.write(bb)
                    bb.flip()
                    randomAccessFile.write(bb.array(), bb.position(), bb.limit())
                    bb.clear()
                }
            }

            SwingUtilities.invokeLater {
                progressBar.value = 100
                progressBar.string = "finished: file was written to ${destinationFile.absolutePath}"
            }
        }
        return true
    }

    fun showApp() {
        loadPreferences()

        isVisible = true
    }

    companion object {
        @JvmStatic
        fun main(s: Array<String>) {
            DltFilterApp().showApp()
        }
    }
}
