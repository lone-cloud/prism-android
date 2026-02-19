package app.lonecloud.prism.activities

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.lonecloud.prism.PrismConfig
import app.lonecloud.prism.activities.ui.MainUiState
import app.lonecloud.prism.activities.ui.SettingsState
import org.unifiedpush.android.distributor.ipc.InternalMessenger
import org.unifiedpush.android.distributor.ui.state.DistribMigrationState
import org.unifiedpush.android.distributor.ui.vm.DistribMigrationViewModel

class ViewModelFactory(val application: Application, val messenger: InternalMessenger) : ViewModelProvider.Factory {
    private val requireBatteryOptimization =
        !(application.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(application.packageName)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(requireBatteryOptimization, messenger, application)
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(messenger, application)
        modelClass.isAssignableFrom(ThemeViewModel::class.java) -> ThemeViewModel(messenger, application)
        modelClass.isAssignableFrom(DistribMigrationViewModel::class.java) -> DistribMigrationViewModel(
            DistribMigrationState(),
            PrismConfig,
            messenger
        )
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    } as T
}

class PreviewFactory(val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MainViewModel::class.java) -> {
            MainViewModel(
                MainUiState(),
                org.unifiedpush.android.distributor.ui.vm.BatteryOptimisationViewModel(false, null),
                org.unifiedpush.android.distributor.ui.vm.RegistrationsViewModel(
                    org.unifiedpush.android.distributor.ui.state.RegistrationListState(
                        emptyList<org.unifiedpush.android.distributor.data.App>()
                    ),
                    null
                ),
                null,
                null
            )
        }
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
            SettingsViewModel(
                SettingsState(
                    showToasts = false,
                    pushServiceUrl = "",
                    prismServerUrl = "",
                    prismApiKey = ""
                ),
                null,
                null
            )
        }
        modelClass.isAssignableFrom(ThemeViewModel::class.java) -> ThemeViewModel(null, null)
        modelClass.isAssignableFrom(DistribMigrationViewModel::class.java) -> {
            DistribMigrationViewModel(
                DistribMigrationState(),
                PrismConfig,
                null
            )
        }
        else -> throw IllegalArgumentException("Unknown ViewModel class")
    } as T
}
