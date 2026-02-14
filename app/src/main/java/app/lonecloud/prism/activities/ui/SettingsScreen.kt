package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
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
import org.unifiedpush.android.distributor.ui.compose.DistribMigrationUi
import org.unifiedpush.android.distributor.ui.vm.DistribMigrationViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    themeViewModel: ThemeViewModel,
    migrationViewModel: DistribMigrationViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            migrationViewModel.refreshDistributors()
        }
    }
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PrismServerConfigButton(
            currentUrl = viewModel.state.prismServerUrl,
            currentApiKey = viewModel.state.prismApiKey,
            onConfigure = { url, apiKey ->
                viewModel.updatePrismServerUrl(url)
                viewModel.updatePrismApiKey(apiKey)
            }
        )

        PrismTogglePreference(
            title = stringResource(R.string.app_dropdown_show_toasts),
            checked = viewModel.state.showToasts,
            onCheckedChange = { viewModel.toggleShowToasts() }
        )

        PrismTogglePreference(
            title = stringResource(R.string.dynamic_colors_title),
            checked = themeViewModel.dynamicColors,
            onCheckedChange = { themeViewModel.toggleDynamicColors() }
        )
    }
    if (migrationViewModel.state.canMigrate) {
        DistribMigrationUi(migrationViewModel)
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
