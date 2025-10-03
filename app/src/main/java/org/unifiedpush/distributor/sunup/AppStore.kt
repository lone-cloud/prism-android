package org.unifiedpush.distributor.sunup

import android.content.Context
import org.unifiedpush.distributor.MigrationManager
import org.unifiedpush.distributor.Store

class AppStore(context: Context) :
    Store(context, PREF_NAME),
    MigrationManager.MigrationStore {
    var uaid: String?
        get() = sharedPreferences
            .getString(PREF_UAID, null)
        set(value) = sharedPreferences
            .edit()
            .putOrRemove(PREF_UAID, value)
            .apply()

    var apiUrl: String
        get() = sharedPreferences
            .getString(PREF_API_URL, null) ?: BuildConfig.DEFAULT_API_URL
        set(value) = sharedPreferences
            .edit()
            .putOrRemove(PREF_API_URL, value)
            .apply()

    override var fallbackIntroShown: Boolean
        get() = sharedPreferences
            .getBoolean(PREF_FALLBACK_INTRO_SHOWN, false)
        set(value) = sharedPreferences
            .edit()
            .putBoolean(PREF_FALLBACK_INTRO_SHOWN, value)
            .apply()

    override var fallbackService: String?
        get() = sharedPreferences
            .getString(PREF_FALLBACK_SERVICE, null)
        set(value) = sharedPreferences
            .edit()
            .putOrRemove(PREF_FALLBACK_SERVICE, value)
            .apply()

    /**
     * Whether the registrations have been migrated to another app by the user
     */
    override var migrated: Boolean
        get() = sharedPreferences
            .getBoolean(PREF_MIGRATED, false)
        set(value) = sharedPreferences
            .edit()
            .putBoolean(PREF_MIGRATED, value)
            .apply()

    /**
     * Whether the registrations have been migrated to another app by the user
     */
    override var tempMigrated: Boolean
        get() = sharedPreferences
            .getBoolean(PREF_TEMP_MIGRATED, false)
        set(value) = sharedPreferences
            .edit()
            .putBoolean(PREF_TEMP_MIGRATED, value)
            .apply()

    var dynamicColors: Boolean
        get() = sharedPreferences
            .getBoolean(PREF_DYNAMIC_COLORS, false)
        set(value) = sharedPreferences
            .edit()
            .putBoolean(PREF_DYNAMIC_COLORS, value)
            .apply()

    /**
     * Show toasts when a new app is registered or an error occurred
     */
    var showToasts: Boolean
        get() = sharedPreferences
            .getBoolean(PREF_SHOW_TOASTS, false)
        set(value) = sharedPreferences
            .edit()
            .putBoolean(PREF_SHOW_TOASTS, value)
            .apply()

    override fun wipe() {
        uaid = null
    }

    companion object {
        internal const val PREF_NAME = "Sunup"
        private const val PREF_UAID = "uaid"
        private const val PREF_API_URL = "api_url"
        private const val PREF_FALLBACK_INTRO_SHOWN = "fallback_intro_shown"
        private const val PREF_FALLBACK_SERVICE = "fallback_service"
        private const val PREF_MIGRATED = "migrated"
        private const val PREF_TEMP_MIGRATED = "temp_migrated"
        private const val PREF_DYNAMIC_COLORS = "dynamic_colors"
        private const val PREF_SHOW_TOASTS = "show_toasts"
    }
}
