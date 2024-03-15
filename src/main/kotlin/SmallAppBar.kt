import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.runtime.Composable

@Preview
@Composable
fun SmallAppBar() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    IconButton(
                        onClick = {}
                    ) {
                        Icon(Icons.Rounded.Build, "Options")
                    }
                }
            )
        }
    ) { innerPadding ->
        IconButton(
            onClick = {}
        ) {
            Icon(Icons.Rounded.Build, "Options")
        }
    }
}
