package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import app.lonecloud.prism.EventBus
import app.lonecloud.prism.R
import app.lonecloud.prism.activities.DistribMigrationViewModel
import app.lonecloud.prism.activities.MainViewModel
import app.lonecloud.prism.activities.PreviewFactory
import app.lonecloud.prism.activities.UiAction
import org.unifiedpush.android.distributor.ui.R as LibR
import org.unifiedpush.android.distributor.ui.compose.AppBar
import org.unifiedpush.android.distributor.ui.compose.CardDisableBatteryOptimisation
import org.unifiedpush.android.distributor.ui.compose.CardDisabledForMigration
import org.unifiedpush.android.distributor.ui.compose.DistribMigrationUi
import org.unifiedpush.android.distributor.ui.compose.PermissionsUi
import org.unifiedpush.android.distributor.ui.compose.RegistrationList
import org.unifiedpush.android.distributor.ui.compose.RegistrationListHeading
import org.unifiedpush.android.distributor.ui.compose.UnregisterBarUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBarOrSelection(viewModel: MainViewModel, onGoToSettings: () -> Unit) {
    val registrationsState = viewModel.registrationsViewModel.state
    if (registrationsState.selectionCount > 0) {
        UnregisterBarUi(
            viewModel = viewModel.registrationsViewModel
        )
    } else {
        MainAppBar(
            onGoToSettings
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar(onGoToSettings: () -> Unit) {
    AppBar(
        R.string.app_name,
        false,
        {},
        actions = {
            IconButton(
                onClick = onGoToSettings
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(LibR.string.settings)
                )
            }
        }
    )
}

@Composable
fun MainScreen(viewModel: MainViewModel, migrationViewModel: DistribMigrationViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        EventBus.subscribe<UiAction> {
            it.handle { type ->
                when (type) {
                    UiAction.Action.RefreshRegistrations -> viewModel.refreshRegistrations()
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshRegistrations()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (migrationViewModel.state.migrated) {
                CardDisabledForMigration {
                    migrationViewModel.reactivateUnifiedPush()
                }
                return@Column
            }

            CardDisableBatteryOptimisation(viewModel.batteryOptimisationViewModel)

            RegistrationListHeading(
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    viewModel.addDebugClick()
                }
            )
        }

        RegistrationList(viewModel.registrationsViewModel) {}
    }
    if (viewModel.mainUiState.showPermissionDialog) {
        PermissionsUi {
            viewModel.closePermissionDialog()
            migrationViewModel.mayShowFallbackIntro()
        }
    }
    if (viewModel.mainUiState.showDebugInfo) {
        DebugDialog {
            viewModel.dismissDebugInfo()
        }
    }
    if (viewModel.mainUiState.showAddAppDialog) {
        AddAppDialog(
            installedApps = viewModel.mainUiState.installedApps,
            onDismiss = { viewModel.hideAddAppDialog() },
            onConfirm = { name, packageName, description ->
                viewModel.addApp(name, packageName, description)
            }
        )
    }
    if (migrationViewModel.state.showMigrations) {
        DistribMigrationUi(migrationViewModel)
    }
}

@Preview
@Composable
fun MainPreview() {
    val factory = PreviewFactory(LocalContext.current)
    val mainVM = viewModel<MainViewModel>(factory = factory)
    val migrationVM = viewModel<DistribMigrationViewModel>(factory = factory)
    MainScreen(mainVM, migrationVM)
}
