package org.unifiedpush.distributor.sunup.activities

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.net.URL
import kotlinx.coroutines.launch
import org.unifiedpush.distributor.sunup.AppStore
import org.unifiedpush.distributor.sunup.BuildConfig
import org.unifiedpush.distributor.sunup.activities.ui.SettingsState
import org.unifiedpush.distributor.sunup.utils.TAG

class SettingsViewModel(state: SettingsState, val application: Application? = null) : ViewModel() {
    constructor(application: Application) : this(
        SettingsState.from(application),
        application
    )

    var state by mutableStateOf(state)
        private set

    fun toggleChangeServer() {
        viewModelScope.launch {
            state = state.copy(showChangeServerDialog = !state.showChangeServerDialog)
        }
    }

    fun togglePrivacyPolicy() {
        viewModelScope.launch {
            state = state.copy(showPrivacyPolicy = !state.showPrivacyPolicy)
        }
    }

    fun newPushServer(url: String) {
        var url = url
        viewModelScope.launch {
            try {
                if (url.isBlank()) {
                    url = BuildConfig.DEFAULT_API_URL
                }
                if (url.slice(0..4) !in arrayOf("http:", "https", "ws://", "wss:/")) {
                    url = "https://$url"
                }
                URL(url)
                state = state.copy(
                    currentApiUrl = url,
                    showChangeServerDialog = false
                )
                publishAction(AppAction(AppAction.Action.NewPushServer(url)))
            } catch (e: Exception) {
                Log.d(TAG, "Ignoring url: $url : $e:w")
            }
        }
    }

    fun refreshApiUrl() {
        viewModelScope.launch {
            application?.let {
                state = state.copy(currentApiUrl = AppStore(it).apiUrl)
            }
        }
    }

    fun toggleShowToasts() {
        viewModelScope.launch {
            state = state.copy(showToasts = !state.showToasts)
            publishAction(AppAction(AppAction.Action.ShowToasts(state.showToasts)))
        }
    }
}
