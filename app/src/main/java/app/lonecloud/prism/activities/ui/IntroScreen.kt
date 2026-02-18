package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lonecloud.prism.PrismServerClient
import app.lonecloud.prism.R
import app.lonecloud.prism.activities.ui.components.PasswordTextField
import app.lonecloud.prism.utils.normalizeUrl

@Composable
fun IntroScreen(onComplete: (url: String, apiKey: String) -> Unit, onSkip: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val successMessage = stringResource(R.string.connection_successful)
    val failedMessageTemplate = stringResource(R.string.connection_failed)

    fun testAndSave(normalizedUrl: String) {
        isTesting = true
        PrismServerClient.testConnection(
            normalizedUrl,
            apiKey,
            onSuccess = {
                isTesting = false
                testResult = successMessage
                onComplete(normalizedUrl, apiKey)
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
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(120.dp)
        )

        Text(
            text = stringResource(R.string.intro_welcome_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
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

        Button(
            onClick = {
                if (url.isBlank() || apiKey.isBlank()) {
                    onSkip()
                    return@Button
                }
                val normalizedUrl = normalizeUrl(url)
                testAndSave(normalizedUrl)
            },
            enabled = !isTesting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (url.isNotBlank() && apiKey.isNotBlank()) {
                    stringResource(R.string.test_and_save_button)
                } else {
                    stringResource(R.string.intro_continue_button)
                }
            )
        }

        TextButton(
            onClick = onSkip,
            enabled = !isTesting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.intro_skip_button))
        }
    }
}
