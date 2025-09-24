package org.unifiedpush.distributor.sunup.activities

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.unifiedpush.android.distributor.ui.compose.BatteryOptimisationViewModel
import org.unifiedpush.android.distributor.ui.compose.previewRegistrationsViewModel
import org.unifiedpush.android.distributor.ui.compose.state.DistribMigrationState
import org.unifiedpush.distributor.sunup.BuildConfig
import org.unifiedpush.distributor.sunup.activities.ui.AppBarUiState
import org.unifiedpush.distributor.sunup.activities.ui.MainUiState

class ViewModelFactory(val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel.from(application)
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    } as T
}

class PreviewFactory(val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MainViewModel::class.java) -> {
            val migrationVM = DistribMigrationViewModel(DistribMigrationState())
            MainViewModel(
                MainUiState(),
                migrationViewModel = migrationVM,
                AppBarViewModel(
                    AppBarUiState(BuildConfig.DEFAULT_API_URL, false),
                    migrationVM
                ),
                BatteryOptimisationViewModel(true),
                previewRegistrationsViewModel(context)
            )
        }
        else -> throw IllegalArgumentException("Unknown ViewModel class")
    } as T
}