import analyzer.formatDefault
import analyzer.ui.swing.HintTextFieldUI
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import db.DltTableDataAccess
import db.DltTarget
import java.awt.Color
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingUtilities

@Composable
@Preview
fun RowScope.LogPanel(dltTarget: DltTarget) {
    var appId by rememberSaveable { mutableStateOf("") }


    val textArea = JTextArea("").also {
        it.isEditable = false
        it.background = Color.WHITE
    }
    val textAreaScroll = JScrollPane(textArea)

    Column(Modifier.weight(1f).padding(8.dp)) {
        Row(Modifier.fillMaxWidth(1f)) {
            SwingPanel(
                modifier = Modifier.fillMaxWidth().height(20.dp),
                factory = {
                    JTextField(appId).also { textField ->
                        textField.setUI(HintTextFieldUI("Enter App-ID and press enter", hideOnFocus = true))
                        textField.addActionListener { _ ->
                            appId = textField.text
                            textArea.text = "Loading data"
                        }
                    }
                },
                update = {
                    it.text = appId
                }
            )
        }
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = {
                textAreaScroll
            }
        )
    }

    LaunchedEffect(dltTarget, appId) {
        val dao = DltTableDataAccess(dltTarget.dataSource)
        val appIds = appId.split(',', ' ', ';').filter { it.isNotBlank() }

        val data = dao.readDataPrepared("app_id IN (${appIds.joinToString(",") { "?" }})") {
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
