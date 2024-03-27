package analyzer

import analyzer.ui.dltAnalyzerMainWindow
import androidx.compose.ui.window.application
import org.oxbow.swingbits.dialog.task.TaskDialogs

class DltAnalyzerApp {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Thread.setDefaultUncaughtExceptionHandler { _, e ->
                TaskDialogs.build().showException(e)
            }
            application {
                dltAnalyzerMainWindow(args)
            }
        }
    }
}
