package app.lonecloud.prism.activities

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lonecloud.prism.BuildConfig
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.PrismServerClient
import app.lonecloud.prism.activities.ui.SettingsState
import app.lonecloud.prism.receivers.PrismConfigReceiver
import app.lonecloud.prism.utils.normalizeUrl
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

    private fun sendConfigBroadcast(
        app: Application,
        action: String,
        value: String
    ) {
        val extraKey = when (action) {
            PrismConfigReceiver.ACTION_SET_PUSH_SERVICE_URL -> PrismConfigReceiver.EXTRA_URL
            PrismConfigReceiver.ACTION_SET_PRISM_SERVER_URL -> PrismConfigReceiver.EXTRA_URL
            PrismConfigReceiver.ACTION_SET_PRISM_API_KEY -> PrismConfigReceiver.EXTRA_API_KEY
            else -> return
        }
        val intent = Intent(action).apply {
            putExtra(extraKey, value)
            setPackage(app.packageName)
        }
        app.sendBroadcast(intent)
    }

    private fun shouldTriggerRegistration(): Boolean = state.prismServerUrl.isNotBlank() && state.prismApiKey.isNotBlank()

    fun toggleShowToasts() {
        viewModelScope.launch {
            state = state.copy(showToasts = !state.showToasts)
            application?.let { PrismPreferences(it).showToasts = state.showToasts }
            messenger?.sendIMessage(InternalOpcode.SHOW_TOASTS_SET, if (state.showToasts) 1 else 0)
        }
    }

    fun updatePushServiceUrl(url: String) {
        viewModelScope.launch {
            val normalizedUrl = if (url.isBlank()) {
                BuildConfig.DEFAULT_API_URL
            } else {
                normalizeUrl(url)
            }

            state = state.copy(pushServiceUrl = normalizedUrl)
            application?.let { app ->
                sendConfigBroadcast(app, PrismConfigReceiver.ACTION_SET_PUSH_SERVICE_URL, if (url.isBlank()) "" else normalizedUrl)
            }
        }
    }

    fun updatePrismServerUrl(url: String, sendAction: Boolean = true) {
        viewModelScope.launch {
            val trimmedUrl = url.trim()
            state = state.copy(prismServerUrl = trimmedUrl)
            application?.let { app ->
                PrismPreferences(app).prismServerUrl = trimmedUrl.ifBlank { null }
                sendConfigBroadcast(app, PrismConfigReceiver.ACTION_SET_PRISM_SERVER_URL, trimmedUrl)

                if (shouldTriggerRegistration()) {
                    PrismServerClient.registerAllApps(app)
                }

                if (sendAction) {
                    sendUiAction(app, "UpdatePrismServerConfigured")
                }
            }
        }
    }

    fun updatePrismApiKey(apiKey: String, sendAction: Boolean = true) {
        viewModelScope.launch {
            val trimmedKey = apiKey.trim()
            state = state.copy(prismApiKey = trimmedKey)
            application?.let { app ->
                PrismPreferences(app).prismApiKey = trimmedKey.ifBlank { null }
                sendConfigBroadcast(app, PrismConfigReceiver.ACTION_SET_PRISM_API_KEY, trimmedKey)

                if (shouldTriggerRegistration()) {
                    PrismServerClient.registerAllApps(app)
                }

                if (sendAction) {
                    sendUiAction(app, "UpdatePrismServerConfigured")
                }
            }
        }
    }

    fun savePrismConfig(url: String, apiKey: String) {
        viewModelScope.launch {
            val trimmedUrl = url.trim()
            val trimmedKey = apiKey.trim()

            state = state.copy(
                prismServerUrl = trimmedUrl,
                prismApiKey = trimmedKey
            )

            application?.let { app ->
                PrismPreferences(app).apply {
                    prismServerUrl = trimmedUrl.ifBlank { null }
                    prismApiKey = trimmedKey.ifBlank { null }
                }

                sendConfigBroadcast(app, PrismConfigReceiver.ACTION_SET_PRISM_SERVER_URL, trimmedUrl)
                sendConfigBroadcast(app, PrismConfigReceiver.ACTION_SET_PRISM_API_KEY, trimmedKey)

                if (shouldTriggerRegistration()) {
                    PrismServerClient.registerAllApps(app)
                }

                sendUiAction(app, "UpdatePrismServerConfigured")
            }
        }
    }
}
