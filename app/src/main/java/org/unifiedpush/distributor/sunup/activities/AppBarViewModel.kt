package org.unifiedpush.distributor.sunup.activities

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.net.URL
import kotlinx.coroutines.launch
import org.unifiedpush.distributor.sunup.BuildConfig
import org.unifiedpush.distributor.sunup.activities.ui.AppBarUiState
import org.unifiedpush.distributor.sunup.utils.TAG

class AppBarViewModel(appBarUiState: AppBarUiState) : ViewModel() {

    var state by mutableStateOf(appBarUiState)

    fun toggleMenu() {
        viewModelScope.launch {
            state = state.copy(menuExpanded = !state.menuExpanded)
        }
    }

    fun toggleChangeServer() {
        viewModelScope.launch {
            state = state.copy(showChangeServerDialog = !state.showChangeServerDialog)
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
                publishAction(AppAction(AppAction.Action.NewPushServer(url)))
                state = state.copy(
                    showChangeServerDialog = false,
                    currentApiUrl = url
                )
            } catch (e: Exception) {
                Log.d(TAG, "Ignoring url: $url : $e:w")
            }
        }
    }
}
