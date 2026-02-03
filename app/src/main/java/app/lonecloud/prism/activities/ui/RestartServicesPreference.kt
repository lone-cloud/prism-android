package app.lonecloud.prism.activities.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.lonecloud.prism.R

@Composable
fun RestartServicesPreference(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(stringResource(R.string.restart_service_button))
    }
}
