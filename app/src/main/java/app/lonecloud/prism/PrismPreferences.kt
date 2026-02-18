package app.lonecloud.prism

import android.content.Context
import androidx.core.content.edit
import org.unifiedpush.android.distributor.MigrationManager
import org.unifiedpush.android.distributor.Store

class PrismPreferences(context: Context) :
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

    var introCompleted: Boolean
        get() = sharedPreferences
            .getBoolean(PREF_INTRO_COMPLETED, false)
        set(value) = sharedPreferences
            .edit {
                putBoolean(PREF_INTRO_COMPLETED, value)
            }

    fun getPrismServerConfig(): Pair<String, String>? {
        val url = prismServerUrl?.takeIf { it.isNotBlank() } ?: return null
        val key = prismApiKey?.takeIf { it.isNotBlank() } ?: return null
        return url to key
    }

    fun setSubscriptionId(connectorToken: String, subscriptionId: String) {
        sharedPreferences.edit {
            putString("subscription_id_$connectorToken", subscriptionId)
        }
    }

    fun getSubscriptionId(connectorToken: String): String? {
        val key = "subscription_id_$connectorToken"

        return try {
            sharedPreferences.getString(key, null)
        } catch (_: ClassCastException) {
            val legacyId = sharedPreferences.getLong(key, -1L)
            if (legacyId == -1L) {
                null
            } else {
                val asString = legacyId.toString()
                setSubscriptionId(connectorToken, asString)
                asString
            }
        }
    }

    fun removeSubscriptionId(connectorToken: String) {
        sharedPreferences.edit {
            remove("subscription_id_$connectorToken")
        }
    }

    fun setRegisteredEndpoint(connectorToken: String, endpoint: String) {
        sharedPreferences.edit {
            putString("registered_endpoint_$connectorToken", endpoint)
        }
    }

    fun getRegisteredEndpoint(connectorToken: String): String? =
        sharedPreferences.getString("registered_endpoint_$connectorToken", null)

    fun removeRegisteredEndpoint(connectorToken: String) {
        sharedPreferences.edit {
            remove("registered_endpoint_$connectorToken")
        }
    }

    fun setVapidPrivateKey(connectorToken: String, privateKey: String) {
        sharedPreferences.edit {
            putString("vapid_private_$connectorToken", privateKey)
        }
    }

    fun getVapidPrivateKey(connectorToken: String): String? =
        sharedPreferences.getString("vapid_private_$connectorToken", null)

    fun removeVapidPrivateKey(connectorToken: String) {
        sharedPreferences.edit {
            remove("vapid_private_$connectorToken")
        }
    }

    fun setRegistrationAddedAt(connectorToken: String, timestampMs: Long) {
        sharedPreferences.edit {
            putLong("registration_added_at_$connectorToken", timestampMs)
        }
    }

    fun getRegistrationAddedAt(connectorToken: String): Long? {
        val timestamp = sharedPreferences.getLong("registration_added_at_$connectorToken", -1L)
        return if (timestamp == -1L) null else timestamp
    }

    fun removeRegistrationAddedAt(connectorToken: String) {
        sharedPreferences.edit {
            remove("registration_added_at_$connectorToken")
        }
    }

    fun cleanupLegacyRegistrationAddedAtIfNeeded() {
        if (sharedPreferences.getBoolean(PREF_ADDED_AT_CLEANED_UP, false)) {
            return
        }

        val keysToRemove = sharedPreferences.all.keys
            .filter { it.startsWith("registration_added_at_") }

        sharedPreferences.edit {
            keysToRemove.forEach { remove(it) }
            putBoolean(PREF_ADDED_AT_CLEANED_UP, true)
        }
    }

    fun addPendingManualToken(connectorToken: String) {
        val tokens = sharedPreferences.getStringSet(PREF_PENDING_MANUAL_TOKENS, emptySet())
            ?.toMutableSet()
            ?: mutableSetOf()
        tokens.add(connectorToken)
        sharedPreferences.edit(commit = true) {
            putStringSet(PREF_PENDING_MANUAL_TOKENS, tokens)
        }
    }

    fun removePendingManualToken(connectorToken: String) {
        val tokens = sharedPreferences.getStringSet(PREF_PENDING_MANUAL_TOKENS, emptySet())
            ?.toMutableSet()
            ?: mutableSetOf()
        tokens.remove(connectorToken)
        sharedPreferences.edit(commit = true) {
            putStringSet(PREF_PENDING_MANUAL_TOKENS, tokens)
        }
    }

    fun isPendingManualToken(connectorToken: String): Boolean =
        sharedPreferences.getStringSet(PREF_PENDING_MANUAL_TOKENS, emptySet())?.contains(connectorToken) == true

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
        private const val PREF_INTRO_COMPLETED = "intro_completed"
        private const val PREF_PENDING_MANUAL_TOKENS = "pending_manual_tokens"
        private const val PREF_ADDED_AT_CLEANED_UP = "added_at_cleaned_up"
    }
}
