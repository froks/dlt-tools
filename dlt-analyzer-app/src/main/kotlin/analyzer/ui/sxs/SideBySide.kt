package analyzer.ui.sxs

import LogPanel
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import db.DltTarget

@Composable
@Preview
fun SideBySide(dltTarget: DltTarget) {
    Row(modifier = Modifier.padding(8.dp).fillMaxSize()) {
        LogPanel(dltTarget)
    }
}
