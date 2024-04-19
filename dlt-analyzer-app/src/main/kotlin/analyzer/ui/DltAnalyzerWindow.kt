package analyzer.ui

import analyzer.ui.helper.DltFileChooser
import androidx.compose.runtime.NoLiveLiterals
import com.formdev.flatlaf.FlatDarculaLaf
import db.DltManager
import db.DltTarget
import jiconfont.icons.font_awesome.FontAwesome
import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons
import jiconfont.swing.IconFontSwing
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.io.File
import javax.swing.*
import kotlin.math.roundToInt

private val logger = LoggerFactory.getLogger("dlt-analyzer")

@NoLiveLiterals
class DltAnalyzerWindow {
    private var frame: JFrame
    private var mainTabbedPane: DltMainTabbedPane
    private var progressBar: JProgressBar

    init {
        FlatDarculaLaf.setup()
        IconFontSwing.register(GoogleMaterialDesignIcons.getIconFont())
        IconFontSwing.register(FontAwesome.getIconFont())

        frame = JFrame("dlt-analyzer")

        val open = JMenuItem("Open...", 'O'.code)
        open.addActionListener {
            val f = DltFileChooser("Select dlt file", null, null).showDialog(null)
            if (f != null) {
                openFile(f)
            }
        }
        val exit = JMenuItem("Exit", 'X'.code)
        exit.addActionListener {
            frame.dispose()
        }

        val fileMenu = JMenu("File")
        fileMenu.mnemonic = 'F'.code
        fileMenu.add(open)
        fileMenu.add(JSeparator())
        fileMenu.add(exit)

        val menuBar = JMenuBar()
        menuBar.add(fileMenu)

        frame.jMenuBar = menuBar
        frame.size = Dimension(1200, 800)
        frame.minimumSize = Dimension(800, 600)
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.iconImage = IconFontSwing.buildImage(GoogleMaterialDesignIcons.APPS, 20f, Color.orange)
        frame.setLocationRelativeTo(null)

        mainTabbedPane = DltMainTabbedPane()
        frame.add(mainTabbedPane, BorderLayout.CENTER)

        progressBar = JProgressBar(0, 100)
        progressBar.string = "Please select file"
        progressBar.isStringPainted = true
        progressBar.isVisible = true
        frame.add(progressBar, BorderLayout.SOUTH)
    }

    private fun openFile(file: File) {
        SwingUtilities.invokeLater {
            progressBar.string = "Loading file"
            progressBar.isStringPainted = true
        }
        DltManager.openFile(
            dltFile = file,
            waitForCompleteDatabase = false,
            force = false,
            onFinished = { dltTarget ->
                SwingUtilities.invokeLater {
                    logger.info("Loading finished")
                    progressBar.string = "File: ${dltTarget.dltFile?.absolutePath}"
                    progressBar.isStringPainted = true
                    progressBar.isVisible = false
                }
                setDltTarget(dltTarget)
            }
        ) { progress ->
            if (progress.progress != null) {
                SwingUtilities.invokeLater {
                    progressBar.isIndeterminate = false
                    progressBar.value = (progress.progress!! * 100f).roundToInt()
                    progressBar.string = progress.progressText
                }
            } else if (!progressBar.isIndeterminate) {
                SwingUtilities.invokeLater {
                    progressBar.string = progress.progressText
                    progressBar.isIndeterminate = true
                }
            }
        }
    }

    private fun setDltTarget(dltTarget: DltTarget) {
        logger.info("Setting dlt target")
        mainTabbedPane.setDltTarget(dltTarget)
    }

    fun showApp(args: Array<String>) {
        val autoOpenFile = if (args.size == 1) args[0] else System.getenv("DLT_FILE_AUTOOPEN")
        if (autoOpenFile != null) {
            val file = File(autoOpenFile)
            if (file.exists()) {
                openFile(file)
            } else {
                logger.info("File '${file.absolutePath} does not exist'")
            }
        }
        SwingUtilities.invokeLater {
            frame.isVisible = true
        }
    }
}
