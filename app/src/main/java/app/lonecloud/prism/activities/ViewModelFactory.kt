package app.lonecloud.prism.activities

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.lonecloud.prism.activities.ThemeViewModel
import app.lonecloud.prism.activities.ui.MainUiState
import app.lonecloud.prism.activities.ui.SettingsState
import org.unifiedpush.android.distributor.ui.compose.BatteryOptimisationViewModel
import org.unifiedpush.android.distributor.ui.compose.previewRegistrationsViewModel
import org.unifiedpush.android.distributor.ui.compose.state.DistribMigrationState

class ViewModelFactory(val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(application)
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
            application
        )
        modelClass.isAssignableFrom(ThemeViewModel::class.java) -> ThemeViewModel(application)
        modelClass.isAssignableFrom(DistribMigrationViewModel::class.java) -> DistribMigrationViewModel(application)
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    } as T
}

class PreviewFactory(val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
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
                    showToasts = false,
                    prismServerUrl = "",
                    prismApiKey = ""
                )
            )
        }
        modelClass.isAssignableFrom(ThemeViewModel::class.java) -> ThemeViewModel()
        modelClass.isAssignableFrom(DistribMigrationViewModel::class.java) -> {
            DistribMigrationViewModel(DistribMigrationState())
        }
        else -> throw IllegalArgumentException("Unknown ViewModel class")
    } as T
}
