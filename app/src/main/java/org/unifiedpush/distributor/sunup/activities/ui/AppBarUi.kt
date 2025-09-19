package org.unifiedpush.distributor.sunup.activities.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.unifiedpush.android.distributor.ui.R as LibR
import org.unifiedpush.android.distributor.ui.compose.DistribMigrationUi
import org.unifiedpush.distributor.sunup.R
import org.unifiedpush.distributor.sunup.activities.AppAction
import org.unifiedpush.distributor.sunup.activities.AppBarViewModel
import org.unifiedpush.distributor.sunup.activities.publishAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarUi(appBarViewModel: AppBarViewModel) {
    val state = appBarViewModel.state

    TopAppBar(
        colors = TopAppBarDefaults
            .topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary
            ),
        title = {
            Text(
                stringResource(R.string.app_name)
            )
        },
        actions = {
            IconButton(
                onClick = {
                    appBarViewModel.toggleMenu()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(LibR.string.app_bar_dropdown_description)
                )
            }
            Dropdown(
                state.menuExpanded,
                state.showToasts,
                state.showMigrations,
                onRestart = {
                    appBarViewModel.publishAction(AppAction(AppAction.Action.RestartService))
                    appBarViewModel.toggleMenu()
                },
                onDismiss = {
                    appBarViewModel.toggleMenu()
                },
                onChangeServer = {
                    appBarViewModel.toggleChangeServer()
                },
                onToggleShowToasts = {
                    appBarViewModel.toggleShowToasts()
                },
                onSetFallbackService = {
                    appBarViewModel.toggleSetFallbackServiceDialog()
                },
                onMigrateToDistrib = {
                    appBarViewModel.toggleMigrationDialog()
                }
            )
        }
    )

    if (state.showChangeServerDialog) {
        ChangeServerDialog(
            state.currentApiUrl,
            onDismissRequest = { appBarViewModel.toggleChangeServer() },
            onConfirmation = { appBarViewModel.newPushServer(it) }
        )
    }
    if (state.showMigrations) {
        DistribMigrationUi(appBarViewModel.migrationViewModel)
    }
}

@Composable
fun Dropdown(
    expanded: Boolean,
    showToasts: Boolean,
    showMigrations: Boolean,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
    onChangeServer: () -> Unit,
    onToggleShowToasts: () -> Unit,
    onSetFallbackService: () -> Unit,
    onMigrateToDistrib: () -> Unit
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
            onClick = {
                onChangeServer()
                onDismiss()
            },
            text = {
                Text(
                    stringResource(LibR.string.app_dropdown_change_server)
                )
            }
        )
        DropdownMenuItem(
            onClick = onToggleShowToasts,
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(LibR.string.app_dropdown_show_toasts))
                    Spacer(Modifier.weight(1f))
                    Checkbox(
                        showToasts,
                        onCheckedChange = { onToggleShowToasts() }
                    )
                }
            }
        )
        if (showMigrations) {
            DropdownMenuItem(
                onClick = {
                    onSetFallbackService()
                    onDismiss()
                },
                text = {
                    Text(
                        stringResource(LibR.string.dialog_fallback_title)
                    )
                }
            )
            DropdownMenuItem(
                onClick = {
                    onMigrateToDistrib()
                    onDismiss()
                },
                text = {
                    Text(
                        color = MaterialTheme.colorScheme.error,
                        text = stringResource(LibR.string.dialog_migration_title)
                    )
                }
            )
        }
    }
}
