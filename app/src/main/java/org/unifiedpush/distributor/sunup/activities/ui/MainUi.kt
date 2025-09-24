package org.unifiedpush.distributor.sunup.activities.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.unifiedpush.android.distributor.ui.compose.BatteryOptimisationViewModel
import org.unifiedpush.android.distributor.ui.compose.CardDisableBatteryOptimisation
import org.unifiedpush.android.distributor.ui.compose.CardDisabledForMigration
import org.unifiedpush.android.distributor.ui.compose.PermissionsUi
import org.unifiedpush.android.distributor.ui.compose.RegistrationList
import org.unifiedpush.android.distributor.ui.compose.RegistrationListHeading
import org.unifiedpush.android.distributor.ui.compose.UnregisterBarUi
import org.unifiedpush.android.distributor.ui.compose.previewRegistrationsViewModel
import org.unifiedpush.android.distributor.ui.compose.state.DistribMigrationState
import org.unifiedpush.distributor.sunup.BuildConfig
import org.unifiedpush.distributor.sunup.EventBus
import org.unifiedpush.distributor.sunup.activities.AppBarViewModel
import org.unifiedpush.distributor.sunup.activities.DistribMigrationViewModel
import org.unifiedpush.distributor.sunup.activities.MainViewModel
import org.unifiedpush.distributor.sunup.activities.UiAction
import org.unifiedpush.distributor.sunup.utils.getDebugInfo

@Composable
fun MainUi(viewModel: MainViewModel) {
    val state = viewModel.mainUiState
    val registrationsState = viewModel.registrationsViewModel.state

    if (state.showPermissionDialog) {
        PermissionsUi {
            viewModel.closePermissionDialog()
        }
    }

    Scaffold(
        topBar = {
            if (registrationsState.selectionCount > 0) {
                UnregisterBarUi(
                    viewModel = viewModel.registrationsViewModel,
                    onDelete = { viewModel.deleteSelection() }
                )
            } else {
                AppBarUi(viewModel.appBarViewModel)
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        MainUiContent(viewModel, innerPadding)
    }
}

@Composable
fun MainUiContent(viewModel: MainViewModel, innerPadding: PaddingValues) {
    LaunchedEffect(Unit) {
        EventBus.subscribe<UiAction> {
            it.handle { type ->
                when (type) {
                    UiAction.Action.RefreshRegistrations -> viewModel.refreshRegistrations()
                    UiAction.Action.RefreshApiUrl -> viewModel.refreshApiUrl()
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(innerPadding),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier)

            if (viewModel.migrationViewModel.state.migrated) {
                CardDisabledForMigration {
                    viewModel.migrationViewModel.reactivateUnifiedPush()
                }
                return
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

        RegistrationList(viewModel.registrationsViewModel) {
            // We don't have copyable endpoint
        }
    }
    if (viewModel.mainUiState.showDebugInfo) {
        DebugDialog {
            viewModel.dismissDebugInfo()
        }
    }
}

@Composable
fun DebugDialog(onDismissRequest: () -> Unit) {
    val text = getDebugInfo()
    AlertDialog(
        title = { Text("Debug") },
        text = {
            SelectionContainer {
                Text(text)
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

@Preview
@Composable
fun MainPreview() {
    val migrationVM = DistribMigrationViewModel(DistribMigrationState())
    MainUi(
        MainViewModel(
            MainUiState(),
            migrationViewModel = migrationVM,
            AppBarViewModel(
                AppBarUiState(BuildConfig.DEFAULT_API_URL, false),
                migrationVM
            ),
            BatteryOptimisationViewModel(true),
            previewRegistrationsViewModel(LocalContext.current)
        )
    )
}
