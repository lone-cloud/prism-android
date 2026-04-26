package app.lonecloud.prism.activities.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.lonecloud.prism.R
import app.lonecloud.prism.utils.isValidUrl

@Composable
fun UrlTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isError = value.isNotBlank() && !isValidUrl(value)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier,
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = if (isError) ({ Text(stringResource(R.string.invalid_url)) }) else null
    )
}
