package org.unifiedpush.distributor.sunup.activities.ui

import android.content.Context
import org.unifiedpush.distributor.sunup.AppStore

data class AppBarUiState(
    val currentApiUrl: String,
    val showToasts: Boolean,
    val menuExpanded: Boolean = false,
    val showChangeServerDialog: Boolean = false
) {
    companion object {
        fun from(context: Context): AppBarUiState {
            val store = AppStore(context)
            return AppBarUiState(
                store.apiUrl,
                store.showToasts
            )
        }
    }
}
