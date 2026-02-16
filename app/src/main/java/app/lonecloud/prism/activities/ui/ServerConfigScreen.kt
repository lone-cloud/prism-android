package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lonecloud.prism.R
import app.lonecloud.prism.activities.ui.components.PasswordTextField
import app.lonecloud.prism.utils.normalizeUrl
import app.lonecloud.prism.utils.testServerConnection

@Composable
fun ServerConfigScreen(
    initialUrl: String,
    initialApiKey: String,
    onNavigateBack: () -> Unit,
    onSave: (url: String, apiKey: String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var apiKey by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var showServerChangeWarning by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val successMessage = stringResource(R.string.connection_successful)
    val failedMessageTemplate = stringResource(R.string.connection_failed)

    fun testAndSave(normalizedUrl: String) {
        isTesting = true
        testServerConnection(
            normalizedUrl,
            apiKey,
            onSuccess = {
                isTesting = false
                testResult = successMessage
                onSave(normalizedUrl, apiKey)
                onNavigateBack()
            },
            onError = { error ->
                isTesting = false
                testResult = failedMessageTemplate.replace("%s", error)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.prism_server_info),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PrismInfoWithLink(uriHandler = uriHandler)

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

        PasswordTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                testResult = null
            },
            label = stringResource(R.string.prism_api_key_label),
            placeholder = stringResource(R.string.prism_api_key_placeholder),
            modifier = Modifier.fillMaxWidth(),
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

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (initialUrl.isNotBlank()) {
                OutlinedButton(
                    onClick = { showClearConfirmation = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.clear_server_button))
                }
            }

            Button(
                onClick = {
                    if (url.isBlank() || (apiKey.isBlank() && initialApiKey.isBlank())) {
                        onNavigateBack()
                        return@Button
                    }

                    val normalizedUrl = normalizeUrl(url)
                    val isServerChanging = initialUrl.isNotBlank() && normalizedUrl != initialUrl

                    if (isServerChanging) {
                        val db = app.lonecloud.prism.DatabaseFactory.getDb(context)
                        val manualAppsCount = db.listApps()
                            .count { it.description?.startsWith("target:") == true }
                        if (manualAppsCount > 0) {
                            showServerChangeWarning = true
                            return@Button
                        }
                    }

                    testAndSave(normalizedUrl)
                },
                enabled = !isTesting && url.isNotBlank() && apiKey.isNotBlank(),
                modifier = if (initialUrl.isNotBlank()) Modifier.weight(1f) else Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.test_and_save_button))
            }
        }
    }

    if (showServerChangeWarning) {
        ServerChangeWarningDialog(
            newUrl = normalizeUrl(url),
            initialUrl = initialUrl,
            onConfirm = { normalizedUrl ->
                showServerChangeWarning = false
                testAndSave(normalizedUrl)
            },
            onDismiss = { showServerChangeWarning = false }
        )
    }

    if (showClearConfirmation) {
        ClearServerConfirmationDialog(
            initialUrl = initialUrl,
            onConfirm = {
                showClearConfirmation = false
                onSave("", "")
                onNavigateBack()
            },
            onDismiss = { showClearConfirmation = false }
        )
    }
}

@Composable
internal fun PrismInfoWithLink(uriHandler: androidx.compose.ui.platform.UriHandler) {
    val learnMore = stringResource(R.string.prism_server_learn_more)

    Row(
        modifier = Modifier.clickable {
            uriHandler.openUri("https://github.com/lone-cloud/prism")
        },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = learnMore,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ServerChangeWarningDialog(
    newUrl: String,
    initialUrl: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = app.lonecloud.prism.DatabaseFactory.getDb(context)
    val manualAppsCount = db.listApps()
        .count { it.description?.startsWith("target:") == true }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Prism Server?") },
        text = {
            Text(
                "You have $manualAppsCount manual app${if (manualAppsCount == 1) "" else "s"}" +
                    " registered with the current server.\n\n" +
                    "Changing to $newUrl will delete registrations from the old server" +
                    " and re-register with the new one."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val oldKey = app.lonecloud.prism.PrismPreferences(context).prismApiKey
                    if (initialUrl.isNotBlank() && !oldKey.isNullOrBlank()) {
                        app.lonecloud.prism.PrismServerClient.deleteAllApps(
                            context,
                            serverUrl = initialUrl,
                            apiKey = oldKey
                        )
                    }
                    onConfirm(newUrl)
                }
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ClearServerConfirmationDialog(
    initialUrl: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = app.lonecloud.prism.DatabaseFactory.getDb(context)
    val manualAppsCount = db.listApps()
        .count { it.description?.startsWith("target:") == true }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_server_confirm_title)) },
        text = {
            Text(
                if (manualAppsCount > 0) {
                    stringResource(
                        R.string.clear_server_confirm_message_with_apps,
                        manualAppsCount
                    )
                } else {
                    stringResource(R.string.clear_server_confirm_message_no_apps)
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val oldKey = app.lonecloud.prism.PrismPreferences(context).prismApiKey
                    if (initialUrl.isNotBlank() && !oldKey.isNullOrBlank()) {
                        app.lonecloud.prism.PrismServerClient.deleteAllApps(
                            context,
                            serverUrl = initialUrl,
                            apiKey = oldKey
                        )
                    }
                    onConfirm()
                }
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
