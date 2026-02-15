package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

@Composable
fun AddAppScreen(
    selectedApp: InstalledApp?,
    onNavigateBack: () -> Unit,
    onNavigateToAppPicker: () -> Unit,
    onConfirm: (name: String, packageName: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }

    // Auto-fill name when app is selected
    androidx.compose.runtime.LaunchedEffect(selectedApp) {
        if (name.isBlank() && selectedApp != null) {
            name = selectedApp.appName
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
            onClick = onNavigateToAppPicker,
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

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onConfirm(
                        name.trim(),
                        selectedApp?.packageName ?: "",
                        null
                    )
                    onNavigateBack()
                }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.add_button))
        }
    }
}
