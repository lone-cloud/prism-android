package org.unifiedpush.distributor.sunup.activities

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.unifiedpush.android.distributor.ui.compose.BatteryOptimisationViewModel
import org.unifiedpush.android.distributor.ui.compose.RegistrationsViewModel
import org.unifiedpush.android.distributor.ui.compose.state.RegistrationListState
import org.unifiedpush.distributor.sunup.AppStore
import org.unifiedpush.distributor.sunup.activities.ui.MainUiState

class MainViewModel(
    mainUiState: MainUiState,
    val migrationViewModel: DistribMigrationViewModel,
    val appBarViewModel: AppBarViewModel,
    val batteryOptimisationViewModel: BatteryOptimisationViewModel,
    val registrationsViewModel: RegistrationsViewModel,
    val application: Application? = null
) : ViewModel() {

    var mainUiState by mutableStateOf(mainUiState)
        private set

    private var lastDebugClickTime by mutableLongStateOf(0L)

    private var debugClickCount by mutableIntStateOf(0)

    fun closePermissionDialog() {
        viewModelScope.launch {
            mainUiState = mainUiState.copy(showPermissionDialog = false)
        }
        appBarViewModel.migrationViewModel.mayShowFallbackIntro()
    }

    fun refreshRegistrations() {
        viewModelScope.launch {
            application?.let {
                registrationsViewModel.state = getRegistrationListState(it)
            }
        }
    }

    fun refreshApiUrl() {
        viewModelScope.launch {
            application?.let {
                appBarViewModel.state = appBarViewModel.state.copy(currentApiUrl = AppStore(it).apiUrl)
            }
        }
    }

    fun deleteSelection() {
        viewModelScope.launch {
            val state = registrationsViewModel.state
            val tokenList = state.list.filter { it.selected }.map { it.token }
            publishAction(
                AppAction(AppAction.Action.DeleteRegistration(tokenList))
            )
            registrationsViewModel.state = RegistrationListState(
                list = state.list.filter {
                    !it.selected
                },
                selectionCount = 0
            )
        }
    }

    fun addDebugClick() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDebugClickTime < 500) {
            debugClickCount++
            if (debugClickCount == 5) {
                mainUiState = mainUiState.copy(showDebugInfo = true)
            }
        } else {
            debugClickCount = 1
        }
        lastDebugClickTime = currentTime
    }

    fun dismissDebugInfo() {
        mainUiState = mainUiState.copy(showDebugInfo = false)
    }

    companion object {
        fun from(application: Application): MainViewModel {
            val migrationViewModel = DistribMigrationViewModel(application)
            return MainViewModel(
                mainUiState = MainUiState(),
                migrationViewModel = migrationViewModel,
                appBarViewModel = AppBarViewModel(application, migrationViewModel),
                batteryOptimisationViewModel = BatteryOptimisationViewModel(application),
                registrationsViewModel = RegistrationsViewModel(
                    getRegistrationListState(application)
                ),
                application
            )
        }
    }
}
