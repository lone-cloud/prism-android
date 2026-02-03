package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lonecloud.prism.R
import app.lonecloud.prism.activities.DistribMigrationViewModel
import app.lonecloud.prism.activities.PreviewFactory
import app.lonecloud.prism.activities.SettingsViewModel
import app.lonecloud.prism.activities.ThemeViewModel
import org.unifiedpush.android.distributor.ui.compose.DistribMigrationUi
import org.unifiedpush.android.distributor.ui.compose.DynamicColorsPreferences
import org.unifiedpush.android.distributor.ui.compose.Heading
import org.unifiedpush.android.distributor.ui.compose.MigrationPreferences
import org.unifiedpush.android.distributor.ui.compose.ShowToastsPreference

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    themeViewModel: ThemeViewModel,
    migrationViewModel: DistribMigrationViewModel
) {
    val state = viewModel.state
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            migrationViewModel.refreshDistributors()
        }
    }
    Column(
        modifier = Modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Heading(R.string.app_name)

        ShowToastsPreference(viewModel.state.showToasts) {
            viewModel.toggleShowToasts()
        }

        DynamicColorsPreferences(themeViewModel.dynamicColors) {
            themeViewModel.toggleDynamicColors()
        }

        MigrationPreferences(migrationViewModel)

        PrismServerConfigButton(
            currentUrl = viewModel.state.prismServerUrl,
            onConfigure = { url, apiKey ->
                viewModel.updatePrismServerUrl(url)
                viewModel.updatePrismApiKey(apiKey)
            }
        )

        RestartServicesPreference {
            viewModel.restartService()
        }
    }
    if (migrationViewModel.state.showMigrations) {
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
