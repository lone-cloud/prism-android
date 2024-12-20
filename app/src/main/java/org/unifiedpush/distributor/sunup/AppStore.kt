package org.unifiedpush.distributor.sunup

import android.content.Context
import org.unifiedpush.distributor.Store

class AppStore(context: Context) : Store(context, PREF_NAME) {
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

    override fun wipe() {
        uaid = null
    }

    companion object {
        internal const val PREF_NAME = "Sunup"
        private const val PREF_UAID = "uaid"
        private const val PREF_API_URL = "api_url"
    }
}
