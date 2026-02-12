package app.lonecloud.prism

import android.content.Context
import androidx.core.content.edit
import org.unifiedpush.android.distributor.MigrationManager
import org.unifiedpush.android.distributor.Store

class AppStore(context: Context) :
    Store(context, PREF_NAME),
    MigrationManager.MigrationStore {
    var uaid: String?
        get() = sharedPreferences
            .getString(PREF_UAID, null)
        set(value) = sharedPreferences
            .edit {
                putOrRemove(PREF_UAID, value)
            }

    var apiUrl: String
        get() = sharedPreferences
            .getString(PREF_API_URL, null) ?: BuildConfig.DEFAULT_API_URL
        set(value) = sharedPreferences
            .edit {
                putOrRemove(PREF_API_URL, value)
            }

    override var fallbackIntroShown: Boolean
        get() = sharedPreferences
            .getBoolean(PREF_FALLBACK_INTRO_SHOWN, false)
        set(value) = sharedPreferences
            .edit {
                putBoolean(PREF_FALLBACK_INTRO_SHOWN, value)
            }

    override var fallbackService: String?
        get() = sharedPreferences
            .getString(PREF_FALLBACK_SERVICE, null)
        set(value) = sharedPreferences
            .edit {
                putOrRemove(PREF_FALLBACK_SERVICE, value)
            }

    /**
     * Whether the registrations have been migrated to another app by the user
     */
    override var migrated: Boolean
        get() = sharedPreferences
            .getBoolean(PREF_MIGRATED, false)
        set(value) = sharedPreferences
            .edit {
                putBoolean(PREF_MIGRATED, value)
            }

    /**
     * Whether the registrations have been migrated to another app by the user
     */
    override var tempMigrated: Boolean
        get() = sharedPreferences
            .getBoolean(PREF_TEMP_MIGRATED, false)
        set(value) = sharedPreferences
            .edit {
                putBoolean(PREF_TEMP_MIGRATED, value)
            }

    var dynamicColors: Boolean
        get() = sharedPreferences
            .getBoolean(PREF_DYNAMIC_COLORS, false)
        set(value) = sharedPreferences
            .edit {
                putBoolean(PREF_DYNAMIC_COLORS, value)
            }

    /**
     * Show toasts when a new app is registered or an error occurred
     */
    var showToasts: Boolean
        get() = sharedPreferences
            .getBoolean(PREF_SHOW_TOASTS, false)
        set(value) = sharedPreferences
            .edit {
                putBoolean(PREF_SHOW_TOASTS, value)
            }

    var prismServerUrl: String?
        get() = sharedPreferences
            .getString(PREF_PRISM_SERVER_URL, null)
        set(value) = sharedPreferences
            .edit {
                putOrRemove(PREF_PRISM_SERVER_URL, value)
            }

    var prismApiKey: String?
        get() = sharedPreferences
            .getString(PREF_PRISM_API_KEY, null)
        set(value) = sharedPreferences
            .edit {
                putOrRemove(PREF_PRISM_API_KEY, value)
            }

    override fun wipe() {
        uaid = null
    }

    companion object {
        internal const val PREF_NAME = "Prism"
        private const val PREF_UAID = "uaid"
        private const val PREF_API_URL = "api_url"
        private const val PREF_FALLBACK_INTRO_SHOWN = "fallback_intro_shown"
        private const val PREF_FALLBACK_SERVICE = "fallback_service"
        private const val PREF_MIGRATED = "migrated"
        private const val PREF_TEMP_MIGRATED = "temp_migrated"
        private const val PREF_DYNAMIC_COLORS = "dynamic_colors"
        private const val PREF_SHOW_TOASTS = "show_toasts"
        private const val PREF_PRISM_SERVER_URL = "prism_server_url"
        private const val PREF_PRISM_API_KEY = "prism_api_key"
    }
}
