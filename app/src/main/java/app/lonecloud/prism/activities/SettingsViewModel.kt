package app.lonecloud.prism.activities

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lonecloud.prism.AppStore
import app.lonecloud.prism.activities.ui.SettingsState
import kotlinx.coroutines.launch

class SettingsViewModel(state: SettingsState, val application: Application? = null) : ViewModel() {
    constructor(application: Application) : this(
        SettingsState.from(application),
        application
    )

    var state by mutableStateOf(state)
        private set

    fun toggleShowToasts() {
        viewModelScope.launch {
            state = state.copy(showToasts = !state.showToasts)
            publishAction(AppAction(AppAction.Action.ShowToasts(state.showToasts)))
        }
    }

    fun updatePrismServerUrl(url: String) {
        viewModelScope.launch {
            val trimmedUrl = url.trim()
            state = state.copy(prismServerUrl = trimmedUrl)
            application?.let {
                AppStore(it).prismServerUrl = trimmedUrl.ifBlank { null }

                if (trimmedUrl.isNotBlank() && state.prismApiKey.isNotBlank()) {
                    publishAction(AppAction(AppAction.Action.RegisterPrismServer))
                }
            }
        }
    }

    fun updatePrismApiKey(apiKey: String) {
        viewModelScope.launch {
            val trimmedKey = apiKey.trim()
            state = state.copy(prismApiKey = trimmedKey)
            application?.let {
                AppStore(it).prismApiKey = trimmedKey.ifBlank { null }

                if (state.prismServerUrl.isNotBlank() && trimmedKey.isNotBlank()) {
                    publishAction(AppAction(AppAction.Action.RegisterPrismServer))
                }
            }
        }
    }

    fun restartService() {
        publishAction(AppAction(AppAction.Action.RestartService))
    }
}
