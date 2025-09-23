package org.unifiedpush.distributor.sunup.activities.ui

import android.content.Context
import org.unifiedpush.distributor.sunup.AppStore
import org.unifiedpush.distributor.sunup.BuildConfig
import org.unifiedpush.distributor.utils.listOtherDistributors

data class AppBarUiState(
    val currentApiUrl: String,
    val showToasts: Boolean,
    val menuExpanded: Boolean = false,
    val showPrivacyPolicy: Boolean = false,
    val showChangeServerDialog: Boolean = false,
    /**
     * Used for the fallback service dialog and migration dialog
     */
    val showMigrations: Boolean = false
) {
    companion object {
        fun from(context: Context): AppBarUiState {
            val store = AppStore(context)
            return AppBarUiState(
                store.apiUrl,
                store.showToasts,
                showMigrations = BuildConfig.SUPPORT_MIGRATIONS && context.listOtherDistributors().any()
            )
        }
    }
}
