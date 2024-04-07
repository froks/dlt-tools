package analyzer

import analyzer.ui.DltAnalyzerWindow
import org.oxbow.swingbits.dialog.task.TaskDialogs

class DltAnalyzerApp {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Thread.setDefaultUncaughtExceptionHandler { _, e ->
                TaskDialogs.build().showException(e)
            }

            DltAnalyzerWindow().showApp(args)
        }
    }
}
