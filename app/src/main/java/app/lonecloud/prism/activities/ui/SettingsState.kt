package app.lonecloud.prism.activities.ui

import android.content.Context
import app.lonecloud.prism.PrismPreferences

data class SettingsState(
    val showToasts: Boolean,
    val prismServerUrl: String,
    val prismApiKey: String
) {
    companion object {
        fun from(context: Context): SettingsState {
            val store = PrismPreferences(context)
            return SettingsState(
                showToasts = store.showToasts,
                prismServerUrl = store.prismServerUrl ?: "",
                prismApiKey = store.prismApiKey ?: ""
            )
        }
    }
}
