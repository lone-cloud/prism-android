package org.unifiedpush.distributor.sunup.activities.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.unifiedpush.android.distributor.ui.compose.AboutHeading
import org.unifiedpush.android.distributor.ui.compose.DistribMigrationUi
import org.unifiedpush.android.distributor.ui.compose.Heading
import org.unifiedpush.android.distributor.ui.compose.OtherDistribHeading
import org.unifiedpush.distributor.sunup.EventBus
import org.unifiedpush.distributor.sunup.R
import org.unifiedpush.distributor.sunup.activities.DistribMigrationViewModel
import org.unifiedpush.distributor.sunup.activities.PreviewFactory
import org.unifiedpush.distributor.sunup.activities.SettingsViewModel
import org.unifiedpush.distributor.sunup.activities.UiAction
import org.unifiedpush.android.distributor.ui.R as LibR

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

        Preference(
            stringResource(LibR.string.show_toasts_label),
            if (state.showToasts) {
                stringResource(LibR.string.clicklabel_show_toasts)
            } else {
                stringResource(LibR.string.cliclabel_not_show_toasts)
            },
            stringResource(LibR.string.show_toasts_description),
            state.showToasts,
            onSelect = {
                viewModel.toggleShowToasts()
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ) {
            Preference(
                stringResource(LibR.string.dynamic_colors),
                stringResource(LibR.string.clicklabel_dynamic_colors),
                switched = themeViewModel.dynamicColors,
                onSelect = {
                    themeViewModel.toggleDynamicColors()
                }
            )
        }

        if (migrationViewModel.state.showMigrations) {
            OtherDistribHeading()

            Preference(
                stringResource(LibR.string.dialog_fallback_title),
                stringResource(LibR.string.clicklabel_select_fallback),
                stringResource(LibR.string.select_fallback_description),
                onSelect = {
                    migrationViewModel.toggleSetFallbackServiceDialog()
                }
            )

            Preference(
                stringResource(LibR.string.dialog_migration_title),
                stringResource(LibR.string.clicklabel_dialog_to_migrate),
                stringResource(LibR.string.migration_description),
                warning = true,
                onSelect = {
                    migrationViewModel.toggleMigrationDialog()
                }
            )
        }

        AboutHeading()
        Preference(
            stringResource(LibR.string.privacy_policy),
            "Todo",
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

@Composable
fun Preference(
    label: String,
    onclickLabel: String,
    description: String? = null,
    switched: Boolean? = null,
    warning: Boolean = false,
    onSelect: () -> Unit
) {
    Row (
        Modifier.clickable(
            true,
            onclickLabel,
            onClick = { onSelect() }
        )
    ) {
        Column(
            Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                style = MaterialTheme.typography.titleMedium,
                text = label,
                color = if (warning) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.Unspecified
                }
            )
            description?.let {
                Text(description)
            } ?: Spacer(Modifier.height(8.dp))
        }
        switched?.let {
            Switch(
                modifier = Modifier
                    .scale(0.8f)
                    .align(Alignment.CenterVertically),
                checked = switched,
                onCheckedChange = { onSelect() }
            )
        }
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