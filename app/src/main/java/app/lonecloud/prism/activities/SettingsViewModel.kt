package app.lonecloud.prism.activities

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.PrismServerClient
import app.lonecloud.prism.activities.ui.SettingsState
import app.lonecloud.prism.receivers.PrismConfigReceiver
import kotlinx.coroutines.launch
import org.unifiedpush.android.distributor.ipc.InternalMessenger
import org.unifiedpush.android.distributor.ipc.InternalOpcode
import org.unifiedpush.android.distributor.ipc.sendUiAction

class SettingsViewModel(
    state: SettingsState,
    val messenger: InternalMessenger?,
    val application: Application? = null
) : ViewModel() {
    constructor(messenger: InternalMessenger?, application: Application) : this(
        SettingsState.from(application),
        messenger,
        application
    )

    var state by mutableStateOf(state)
        private set

    fun toggleShowToasts() {
        viewModelScope.launch {
            state = state.copy(showToasts = !state.showToasts)
            application?.let { PrismPreferences(it).showToasts = state.showToasts }
            messenger?.sendIMessage(InternalOpcode.SHOW_TOASTS_SET, if (state.showToasts) 1 else 0)
        }
    }

    fun updatePrismServerUrl(url: String) {
        viewModelScope.launch {
            val trimmedUrl = url.trim()
            state = state.copy(prismServerUrl = trimmedUrl)
            application?.let {
                PrismPreferences(it).prismServerUrl = trimmedUrl.ifBlank { null }

                val intent = Intent(PrismConfigReceiver.ACTION_SET_PRISM_SERVER_URL).apply {
                    putExtra(PrismConfigReceiver.EXTRA_URL, trimmedUrl)
                    setPackage(it.packageName)
                }
                it.sendBroadcast(intent)

                if (trimmedUrl.isNotBlank() && state.prismApiKey.isNotBlank()) {
                    PrismServerClient.registerAllApps(it)
                }

                sendUiAction(it, "UpdatePrismServerConfigured")
            }
        }
    }

    fun updatePrismApiKey(apiKey: String) {
        viewModelScope.launch {
            val trimmedKey = apiKey.trim()
            state = state.copy(prismApiKey = trimmedKey)
            application?.let {
                PrismPreferences(it).prismApiKey = trimmedKey.ifBlank { null }

                val intent = Intent(PrismConfigReceiver.ACTION_SET_PRISM_API_KEY).apply {
                    putExtra(PrismConfigReceiver.EXTRA_API_KEY, trimmedKey)
                    setPackage(it.packageName)
                }
                it.sendBroadcast(intent)

                if (state.prismServerUrl.isNotBlank() && trimmedKey.isNotBlank()) {
                    PrismServerClient.registerAllApps(it)
                }

                sendUiAction(it, "UpdatePrismServerConfigured")
            }
        }
    }
}
