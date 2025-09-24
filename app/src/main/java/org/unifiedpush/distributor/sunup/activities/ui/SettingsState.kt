package org.unifiedpush.distributor.sunup.activities.ui

import android.content.Context
import org.unifiedpush.distributor.sunup.AppStore

data class SettingsState(
    val currentApiUrl: String,
    val showToasts: Boolean,
    val showChangeServerDialog: Boolean = false,
    val showPrivacyPolicy: Boolean = false
) {
    companion object {
        fun from(context: Context): SettingsState {
            val store = AppStore(context)
            return SettingsState(
                store.apiUrl,
                store.showToasts
            )
        }
    }
}