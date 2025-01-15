package org.unifiedpush.distributor.sunup.activities.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.unifiedpush.android.distributor.ui.R
import org.unifiedpush.distributor.sunup.BuildConfig

@Preview
@Composable
fun ChangeServerUi(currentValue: String = BuildConfig.DEFAULT_API_URL, onValueChange: (String) -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.custom_push_server)
        )
        TextField(
            value = currentValue,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.url)) },
            maxLines = 1
        )
    }
}

@Preview
@Composable
fun ChangeServerDialog(
    currentValue: String = BuildConfig.DEFAULT_API_URL,
    onDismissRequest: () -> Unit = {},
    onConfirmation: (String) -> Unit = {}
) {
    var value by remember { mutableStateOf(currentValue) }
    AlertDialog(
        title = {
            stringResource(R.string.custom_push_server)
        },
        text = {
            ChangeServerUi(value) {
                value = it
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation(value)
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
