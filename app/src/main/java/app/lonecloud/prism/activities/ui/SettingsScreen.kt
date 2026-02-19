package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lonecloud.prism.R
import app.lonecloud.prism.activities.PreviewFactory
import app.lonecloud.prism.activities.SettingsViewModel
import app.lonecloud.prism.activities.ThemeViewModel
import org.unifiedpush.android.distributor.ui.compose.DistribMigrationDialogs
import org.unifiedpush.android.distributor.ui.vm.DistribMigrationViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    themeViewModel: ThemeViewModel,
    migrationViewModel: DistribMigrationViewModel,
    onNavigateToServerConfig: () -> Unit = {},
    onNavigateToPushServiceConfig: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            migrationViewModel.refreshDistributors()
        }
    }
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            onClick = onNavigateToServerConfig,
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.configure_server),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (viewModel.state.prismServerUrl.isNotBlank()) {
                            viewModel.state.prismServerUrl
                        } else {
                            stringResource(R.string.prism_server_not_configured)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            onClick = onNavigateToPushServiceConfig,
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Dns,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.push_service_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = viewModel.state.pushServiceUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        PrismTogglePreference(
            title = stringResource(R.string.app_dropdown_show_toasts),
            description = stringResource(R.string.show_toasts_description),
            icon = Icons.Filled.Notifications,
            checked = viewModel.state.showToasts,
            onCheckedChange = { viewModel.toggleShowToasts() }
        )

        PrismTogglePreference(
            title = stringResource(R.string.dynamic_colors_title),
            description = stringResource(R.string.dynamic_colors_description),
            icon = Icons.Filled.Palette,
            checked = themeViewModel.dynamicColors,
            onCheckedChange = { themeViewModel.toggleDynamicColors() }
        )
    }
    if (migrationViewModel.state.canMigrate) {
        DistribMigrationDialogs(migrationViewModel)
    }
}

@Preview
@Composable
fun PreviewSettingsScreen() {
    val factory = PreviewFactory(LocalContext.current)
    val settVM = viewModel<SettingsViewModel>(factory = factory)
    val themeVM = viewModel<ThemeViewModel>(factory = factory)
    val migrationVM = viewModel<DistribMigrationViewModel>(factory = factory)
    SettingsScreen(settVM, themeVM, migrationVM)
}
