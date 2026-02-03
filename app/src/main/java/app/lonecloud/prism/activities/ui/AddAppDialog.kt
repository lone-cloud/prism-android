package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.lonecloud.prism.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppDialog(
    installedApps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, packageName: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_custom_app_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.app_name_label)) },
                    placeholder = { Text(stringResource(R.string.app_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedCard(
                    onClick = { showAppPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.target_app_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedApp?.appName ?: stringResource(R.string.select_an_app),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        selectedApp?.icon?.let { icon ->
                            val bitmap = icon.toBitmap(48, 48)
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            selectedApp?.packageName ?: "",
                            null
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.add_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )

    if (showAppPicker) {
        AppPickerDialog(
            apps = installedApps,
            onDismiss = { showAppPicker = false },
            onSelect = { app ->
                selectedApp = app
                if (name.isBlank()) {
                    name = app.appName
                }
                showAppPicker = false
            }
        )
    }
}
