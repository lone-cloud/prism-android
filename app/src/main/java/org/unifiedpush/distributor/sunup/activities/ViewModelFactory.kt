package org.unifiedpush.distributor.sunup.activities

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.unifiedpush.android.distributor.ui.compose.BatteryOptimisationViewModel
import org.unifiedpush.android.distributor.ui.compose.previewRegistrationsViewModel
import org.unifiedpush.android.distributor.ui.compose.state.DistribMigrationState
import org.unifiedpush.distributor.sunup.BuildConfig
import org.unifiedpush.distributor.sunup.activities.ui.MainUiState
import org.unifiedpush.distributor.sunup.activities.ui.SettingsState

class ViewModelFactory(val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel.from(application)
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
            application
        )
        modelClass.isAssignableFrom(DistribMigrationViewModel::class.java) -> DistribMigrationViewModel(application)
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    } as T
}

class PreviewFactory(val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MainViewModel::class.java) -> {
            MainViewModel(
                MainUiState(),
                BatteryOptimisationViewModel(true),
                previewRegistrationsViewModel(context)
            )
        }
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
            SettingsViewModel(
                SettingsState(
                BuildConfig.DEFAULT_API_URL,
                    false
                ),
            )
        }
        modelClass.isAssignableFrom(DistribMigrationViewModel::class.java) -> {
            DistribMigrationViewModel(DistribMigrationState())
        }
        else -> throw IllegalArgumentException("Unknown ViewModel class")
    } as T
}