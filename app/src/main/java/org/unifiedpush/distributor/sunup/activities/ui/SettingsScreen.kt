package org.unifiedpush.distributor.sunup.activities.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.unifiedpush.android.distributor.ui.R as LibR
import org.unifiedpush.android.distributor.ui.compose.AboutHeading
import org.unifiedpush.android.distributor.ui.compose.DistribMigrationUi
import org.unifiedpush.android.distributor.ui.compose.DynamicColorsPreferences
import org.unifiedpush.android.distributor.ui.compose.Heading
import org.unifiedpush.android.distributor.ui.compose.MigrationPreferences
import org.unifiedpush.android.distributor.ui.compose.Preference
import org.unifiedpush.android.distributor.ui.compose.ShowToastsPreference
import org.unifiedpush.distributor.sunup.EventBus
import org.unifiedpush.distributor.sunup.R
import org.unifiedpush.distributor.sunup.activities.DistribMigrationViewModel
import org.unifiedpush.distributor.sunup.activities.PreviewFactory
import org.unifiedpush.distributor.sunup.activities.SettingsViewModel
import org.unifiedpush.distributor.sunup.activities.ThemeViewModel
import org.unifiedpush.distributor.sunup.activities.UiAction

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    themeViewModel: ThemeViewModel,
    migrationViewModel: DistribMigrationViewModel
) {
    val state = viewModel.state
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        EventBus.subscribe<UiAction> {
            it.handle { type ->
                when (type) {
                    UiAction.Action.RefreshApiUrl -> viewModel.refreshApiUrl()
                    else -> {}
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshApiUrl()
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

        Preference(
            stringResource(LibR.string.push_server),
            stringResource(LibR.string.clicklabel_select_push_server),
            state.currentApiUrl,
            onSelect = {
                viewModel.toggleChangeServer()
            }
        )

        ShowToastsPreference(viewModel.state.showToasts) {
            viewModel.toggleShowToasts()
        }

        DynamicColorsPreferences(themeViewModel.dynamicColors) {
            themeViewModel.toggleDynamicColors()
        }

        MigrationPreferences(migrationViewModel)

        AboutHeading()

        Preference(
            stringResource(LibR.string.privacy_policy),
            stringResource(LibR.string.open_privacy_policy_clicklabel),
            onSelect = {
                viewModel.togglePrivacyPolicy()
            }
        )
    }

    if (state.showChangeServerDialog) {
        ChangeServerDialog(
            state.currentApiUrl,
            onDismissRequest = { viewModel.toggleChangeServer() },
            onConfirmation = { viewModel.newPushServer(it) }
        )
    }
    if (state.showPrivacyPolicy) {
        PrivacyPolicyDialog {
            viewModel.togglePrivacyPolicy()
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
