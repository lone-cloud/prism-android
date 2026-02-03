package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.lonecloud.prism.R
import app.lonecloud.prism.utils.getDebugInfo

@Composable
fun DebugDialog(onDismissRequest: () -> Unit) {
    val text = getDebugInfo()
    AlertDialog(
        title = { Text(stringResource(R.string.debug_title)) },
        text = {
            SelectionContainer {
                Text(text)
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}
