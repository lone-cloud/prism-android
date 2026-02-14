package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.lonecloud.prism.R

@Composable
fun PrismServerConfigButton(
    currentUrl: String,
    currentApiKey: String,
    onConfigure: (url: String, apiKey: String) -> Unit
) {
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
                    currentUrl
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
            initialApiKey = currentApiKey,
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
    initialApiKey: String,
    onDismiss: () -> Unit,
    onSave: (url: String, apiKey: String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var showServerChangeWarning by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.configure_server)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val description = stringResource(R.string.prism_server_description)
                val repoUrl = stringResource(R.string.prism_server_repo_link)
                val fullText = "$description\n\n$repoUrl"
                val annotatedString = buildAnnotatedString {
                    append(description)
                    append("\n\n")

                    val linkStart = length
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(repoUrl)
                    }
                    addStringAnnotation(
                        tag = "URL",
                        annotation = repoUrl,
                        start = linkStart,
                        end = length
                    )
                }

                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                    }
                )

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
                            val oldKey = app.lonecloud.prism.PrismPreferences(context).prismApiKey
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (initialUrl.isNotBlank()) {
                    TextButton(
                        onClick = { showClearConfirmation = true },
                        enabled = !isTesting
                    ) {
                        Text(
                            text = stringResource(R.string.clear_server_button),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = onDismiss, enabled = !isTesting) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        }
    )

    if (showClearConfirmation) {
        val db = app.lonecloud.prism.DatabaseFactory.getDb(context)
        val manualAppsCount = db.listApps()
            .count { it.description?.startsWith("target:") == true }

        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.clear_server_confirm_title)) },
            text = {
                Text(
                    if (manualAppsCount > 0) {
                        stringResource(R.string.clear_server_confirm_message_with_apps, manualAppsCount)
                    } else {
                        stringResource(R.string.clear_server_confirm_message_no_apps)
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (initialUrl.isNotBlank()) {
                            val oldKey = app.lonecloud.prism.PrismPreferences(context).prismApiKey
                            if (!oldKey.isNullOrBlank() && manualAppsCount > 0) {
                                app.lonecloud.prism.PrismServerClient.deleteAllApps(
                                    context,
                                    serverUrl = initialUrl,
                                    apiKey = oldKey
                                )
                            }
                        }
                        onSave("", "")
                        showClearConfirmation = false
                        onDismiss()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.clear_server_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

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
