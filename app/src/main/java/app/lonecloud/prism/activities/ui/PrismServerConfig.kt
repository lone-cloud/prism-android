package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lonecloud.prism.R

@Composable
fun PrismServerConfigButton(currentUrl: String, onConfigure: (url: String, apiKey: String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.configure_server),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (currentUrl.isNotBlank()) {
                    stringResource(R.string.prism_server_configured, currentUrl)
                } else {
                    stringResource(R.string.prism_server_not_configured)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDialog) {
        PrismServerConfigDialog(
            initialUrl = currentUrl,
            onDismiss = { showDialog = false },
            onSave = { url, apiKey ->
                onConfigure(url, apiKey)
                showDialog = false
            }
        )
    }
}

@Composable
fun PrismServerConfigDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (url: String, apiKey: String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var apiKey by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var showServerChangeWarning by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.configure_server)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        testResult = null
                    },
                    label = { Text(stringResource(R.string.prism_server_url_label)) },
                    placeholder = { Text(stringResource(R.string.prism_server_url_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isTesting
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        testResult = null
                    },
                    label = { Text(stringResource(R.string.prism_api_key_label)) },
                    placeholder = { Text(stringResource(R.string.prism_api_key_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isTesting
                )

                if (isTesting) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.testing_connection))
                    }
                }

                testResult?.let { result ->
                    Text(
                        text = result,
                        color = if (result.contains("successful")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        },
        confirmButton = {
            val successMessage = stringResource(R.string.connection_successful)
            val failedMessageTemplate = stringResource(R.string.connection_failed)
            Button(
                onClick = {
                    if (url.isBlank() || apiKey.isBlank()) {
                        onDismiss()
                        return@Button
                    }
                    val isServerChanging = initialUrl.isNotBlank() && url.trim() != initialUrl
                    if (isServerChanging) {
                        val db = app.lonecloud.prism.DatabaseFactory.getDb(context)
                        val manualAppsCount = db.listApps()
                            .count { it.description?.startsWith("target:") == true }
                        if (manualAppsCount > 0) {
                            val oldUrl = initialUrl
                            val oldKey = app.lonecloud.prism.AppStore(context).prismApiKey
                            if (!oldUrl.isNullOrBlank() && !oldKey.isNullOrBlank()) {
                                app.lonecloud.prism.PrismServerClient.deleteAllApps(
                                    context,
                                    serverUrl = oldUrl,
                                    apiKey = oldKey
                                )
                            }
                            showServerChangeWarning = true
                            return@Button
                        }
                    }

                    isTesting = true
                    app.lonecloud.prism.PrismServerClient.testConnection(
                        url,
                        apiKey,
                        onSuccess = {
                            isTesting = false
                            testResult = successMessage
                            onSave(url, apiKey)
                        },
                        onError = { error ->
                            isTesting = false
                            testResult = failedMessageTemplate.replace("%s", error)
                        }
                    )
                },
                enabled = !isTesting && url.isNotBlank() && apiKey.isNotBlank()
            ) {
                Text(stringResource(R.string.test_and_save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isTesting) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )

    if (showServerChangeWarning) {
        val db = app.lonecloud.prism.DatabaseFactory.getDb(context)
        val manualAppsCount = db.listApps()
            .count { it.description?.startsWith("target:") == true }

        AlertDialog(
            onDismissRequest = { showServerChangeWarning = false },
            title = { Text("Change Prism Server?") },
            text = {
                Text(
                    "You have $manualAppsCount manual app${if (manualAppsCount == 1) "" else "s"}" +
                        " registered with the current server.\n\n" +
                        "Changing to $url will delete registrations from the old server" +
                        " and re-register with the new one."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showServerChangeWarning = false
                        isTesting = true
                        app.lonecloud.prism.PrismServerClient.testConnection(
                            url,
                            apiKey,
                            onSuccess = {
                                isTesting = false
                                testResult = "Connection successful"
                                onSave(url, apiKey)
                            },
                            onError = { error ->
                                isTesting = false
                                testResult = "Connection failed: $error"
                            }
                        )
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerChangeWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
