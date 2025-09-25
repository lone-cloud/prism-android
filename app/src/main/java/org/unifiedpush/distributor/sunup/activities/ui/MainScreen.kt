package org.unifiedpush.distributor.sunup.activities.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.unifiedpush.android.distributor.ui.compose.AppBar
import org.unifiedpush.android.distributor.ui.R as LibR
import org.unifiedpush.android.distributor.ui.compose.CardDisableBatteryOptimisation
import org.unifiedpush.android.distributor.ui.compose.CardDisabledForMigration
import org.unifiedpush.android.distributor.ui.compose.DistribMigrationUi
import org.unifiedpush.android.distributor.ui.compose.PermissionsUi
import org.unifiedpush.android.distributor.ui.compose.RegistrationList
import org.unifiedpush.android.distributor.ui.compose.RegistrationListHeading
import org.unifiedpush.android.distributor.ui.compose.UnregisterBarUi
import org.unifiedpush.distributor.sunup.EventBus
import org.unifiedpush.distributor.sunup.R
import org.unifiedpush.distributor.sunup.activities.DistribMigrationViewModel
import org.unifiedpush.distributor.sunup.activities.MainViewModel
import org.unifiedpush.distributor.sunup.activities.PreviewFactory
import org.unifiedpush.distributor.sunup.activities.UiAction
import org.unifiedpush.distributor.sunup.utils.getDebugInfo

@Composable
fun MainAppBarOrSelection(viewModel: MainViewModel, onGoToSettings: () -> Unit) {
    val registrationsState = viewModel.registrationsViewModel.state
    if (registrationsState.selectionCount > 0) {
        UnregisterBarUi(
            viewModel = viewModel.registrationsViewModel,
            onDelete = { viewModel.deleteSelection() }
        )
    } else {
        MainAppBar(
            viewModel,
            onGoToSettings
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar(viewModel: MainViewModel, onGoToSettings: () -> Unit) {
    var openMenu by remember { mutableStateOf(false) }

    AppBar(
        R.string.app_name,
        false,
        {},
        actions = {
            IconButton(
                onClick = {
                    openMenu = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(LibR.string.app_bar_dropdown_description)
                )
            }
            Dropdown(
                openMenu,
                onRestart = {
                    viewModel.restartService()
                    openMenu = false
                },
                onDismiss = {
                    openMenu = false
                },
                onGoToSettings = onGoToSettings
            )
        }
    )
}

@Composable
fun Dropdown(
    expanded: Boolean,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            onClick = onRestart,
            text = {
                Text(stringResource(LibR.string.app_dropdown_restart))
            }
        )
        DropdownMenuItem(
            onClick = onGoToSettings,
            text = {
                Text(stringResource(LibR.string.settings))
            }
        )
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel, migrationViewModel: DistribMigrationViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        EventBus.subscribe<UiAction> {
            it.handle { type ->
                when (type) {
                    UiAction.Action.RefreshRegistrations -> viewModel.refreshRegistrations()
                    else -> {}
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
        modifier = Modifier
            .fillMaxSize(),
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

            if (migrationViewModel.state.migrated) {
                CardDisabledForMigration {
                    migrationViewModel.reactivateUnifiedPush()
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
    if (migrationViewModel.state.showMigrations) {
        DistribMigrationUi(migrationViewModel)
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
    val factory = PreviewFactory(LocalContext.current)
    val mainVM = viewModel<MainViewModel>(factory = factory)
    val migrationVM = viewModel<DistribMigrationViewModel>(factory = factory)
    MainScreen(mainVM, migrationVM)
}
