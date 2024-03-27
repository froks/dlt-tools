package analyzer.ui

import analyzer.ui.swing.DltFileChooser
import analyzer.ui.sxs.SideBySide
import analyzer.ui.table.DltDataTable
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.formdev.flatlaf.FlatLightLaf
import db.DltManager
import db.DltTarget
import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons
import jiconfont.swing.IconFontSwing
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.io.File
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane

private val logger = LoggerFactory.getLogger("dlt-analyzer")

@Composable
@Preview
fun ApplicationScope.dltAnalyzerMainWindow(args: Array<String>) =
    Window(
        onCloseRequest = ::exitApplication,
        title = "dlt-analyzer",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        FlatLightLaf.setup();
        IconFontSwing.register(GoogleMaterialDesignIcons.getIconFont())


        var dltTarget by remember { mutableStateOf<DltTarget?>(null, policy = neverEqualPolicy()) }
        var dltPercentLoaded by remember { mutableStateOf(-1.0f) }

        fun openFile(file: File): DltTarget {
            return DltManager.openFile(
                dltFile = file,
                waitForCompleteDatabase = false,
                force = false,
                onFinished = {
                    // set dltTarget to a copy, to enforce update
                    dltTarget = dltTarget?.copy()
                }
            ) { progress ->
                dltPercentLoaded = progress.progress ?: 0.0f
            }
        }

        LaunchedEffect(true) {
            val autoOpenFile = if (args.size == 1) args[0] else System.getenv("DLT_FILE_AUTOOPEN")
            if (autoOpenFile != null) {
                val file = File(autoOpenFile)
                if (file.exists()) {
                    dltTarget = openFile(file)
                } else {
                    logger.info("File '${file.absolutePath} does not exist'")
                }
            }
        }

        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item(
                    text = "Open file",
                    mnemonic = 'O',
                    onClick = {
                        val f = DltFileChooser("Select dlt file", null, null).showDialog(null)
                        if (f != null) {
                            dltTarget = openFile(f)
                        }
                    }
                )
            }
        }
        DltAnalyzerMain(dltTarget, dltPercentLoaded)
    }

data class NavigationItem(
    val text: String,
    val icon: ImageVector,
    val content: @Composable RowScope.(DltTarget) -> Unit,
)

val navigationItems = mutableListOf(
    NavigationItem(
        text = "Table",
        icon = Icons.Outlined.GridView,
    ) { dltTarget ->
        val table = DltDataTable(dltTarget)
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val panel = JPanel(BorderLayout())
                panel.add(JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER)
                panel
            },
            update = {
                table.setDltTarget(dltTarget)
            }
        )
    },
    NavigationItem(
        text = "Side-by-Side",
        icon = Icons.Outlined.ViewWeek,
    ) { dltTarget ->
        SideBySide(dltTarget)
    }
)

@Composable
@Preview
fun DltAnalyzerMain(dltTarget: DltTarget?, dltPercentLoaded: Float) {
    var selectedItem by remember { mutableStateOf(0) }

    MaterialTheme {
        Scaffold { padding ->
            Row(Modifier.padding(padding).fillMaxWidth()) {
                if (dltTarget == null) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("Select file to open")
                    }
                } else if (!dltTarget.isLoaded) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        LinearProgressIndicator(progress = { dltPercentLoaded }, modifier = Modifier.fillMaxWidth().height(10.dp))
                    }
                } else {
                    NavigationRail(modifier = Modifier.width(100.dp)) {
                        navigationItems.forEachIndexed { index, item ->
                            NavigationRailItem(
                                label = { Text(item.text) },
                                modifier = Modifier.fillMaxWidth(),
                                icon = {
                                    Icon(imageVector = item.icon, contentDescription = "")
                                },
                                selected = selectedItem == index,
                                onClick = { selectedItem = index },
                            )
                        }
//                        Spacer(modifier = Modifier.weight(1f))
//                        NavigationRailItem(
//                            label = { Text("Settings") },
//                            icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = "") },
//                            selected = false,
//                            onClick = { selectedItem = -1 },
//                        )
                    }

                    when (selectedItem) {
                        -1 -> Text("Settings")
                        else -> navigationItems[selectedItem].content.invoke(this, dltTarget)
                    }
                }
            }
        }

    }
}
