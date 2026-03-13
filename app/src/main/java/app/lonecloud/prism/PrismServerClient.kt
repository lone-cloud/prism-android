package app.lonecloud.prism

import android.content.Context
import android.util.Base64
import android.util.Log
import app.lonecloud.prism.utils.DescriptionParser
import app.lonecloud.prism.utils.HttpClientFactory
import app.lonecloud.prism.utils.redactIdentifier
import app.lonecloud.prism.utils.toBase64Url
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject

object PrismServerClient {
    private const val TAG = "PrismServerClient"

    private data class ServerConfig(val serverUrl: String, val apiKey: String)

    data class WebPushRegistration(
        val connectorToken: String,
        val appName: String,
        val webpushUrl: String,
        val vapidPrivateKey: String? = null,
        val p256dh: String? = null,
        val auth: String? = null
    )

    private fun getAuthHeader(apiKey: String): String = "Bearer $apiKey"

    fun registerApp(
        context: Context,
        registration: WebPushRegistration,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val config = resolveServerConfig(context)
        if (config == null) {
            val error = "Prism server not configured"
            debugLog { "$error, skipping registration" }
            onError(error)
            return
        }

        if (registration.vapidPrivateKey.isNullOrBlank() || !isValidVapidPrivateKey(registration.vapidPrivateKey)) {
            val error = "Invalid VAPID private key for ${registration.appName}. Delete and re-register the app."
            Log.e(TAG, error)
            onError(error)
            return
        }

        val (serverUrl, apiKey) = config

        AppScope.launch {
            try {
                val store = PrismPreferences(context)
                val existingSubscriptionId = store.getSubscriptionId(registration.connectorToken)
                    ?: getSubscriptionIdFromDb(context, registration.connectorToken)?.also {
                        store.setSubscriptionId(registration.connectorToken, it)
                        debugLog { "registerApp: restored subscriptionId for token=${redactIdentifier(registration.connectorToken)}" }
                    }
                val knownEndpoint = store.getRegisteredEndpoint(registration.connectorToken)
                    ?: getEndpointFromDb(context, registration.connectorToken)?.also {
                        store.setRegisteredEndpoint(registration.connectorToken, it)
                        debugLog { "registerApp: restored endpoint for token=${redactIdentifier(registration.connectorToken)}" }
                    }

                debugLog {
                    "registerApp decision: token=${redactIdentifier(
                        registration.connectorToken
                    )} existingSub=${!existingSubscriptionId.isNullOrBlank()} endpointMatch=${knownEndpoint == registration.webpushUrl}"
                }

                if (!existingSubscriptionId.isNullOrBlank() && knownEndpoint == registration.webpushUrl) {
                    debugLog { "registerApp: skipping duplicate create for ${registration.appName} (endpoint unchanged)" }
                    withContext(Dispatchers.Main) { onSuccess() }
                    return@launch
                }

                deleteStaleSubscription(serverUrl, apiKey, store, registration, existingSubscriptionId, knownEndpoint)
                    .onFailure {
                        withContext(Dispatchers.Main) { onError(it.message ?: "Failed to replace subscription") }
                        return@launch
                    }

                postSubscription(serverUrl, apiKey, store, registration, requireNotNull(registration.vapidPrivateKey))
                    .onSuccess {
                        debugLog { "Successfully registered app: ${registration.appName} (ID: ${redactIdentifier(it)})" }
                        withContext(Dispatchers.Main) { onSuccess() }
                    }
                    .onFailure { withContext(Dispatchers.Main) { onError(it.message ?: "Failed to register app") } }
            } catch (e: IOException) {
                val error = "Error registering app: ${e.message}"
                Log.e(TAG, error, e)
                withContext(Dispatchers.Main) { onError(error) }
            }
        }
    }

    private suspend fun deleteStaleSubscription(
        serverUrl: String,
        apiKey: String,
        store: PrismPreferences,
        registration: WebPushRegistration,
        existingSubscriptionId: String?,
        knownEndpoint: String?
    ): Result<Unit> {
        if (existingSubscriptionId.isNullOrBlank() || knownEndpoint.isNullOrBlank() || knownEndpoint == registration.webpushUrl) {
            return Result.success(Unit)
        }
        Log.w(
            TAG,
            "registerApp: endpoint changed for ${registration.appName}, replacing subscription ${redactIdentifier(existingSubscriptionId)}"
        )
        val request = Request.Builder()
            .url("$serverUrl/api/v1/webpush/subscriptions/$existingSubscriptionId")
            .addHeader("Authorization", getAuthHeader(apiKey))
            .delete()
            .build()
        HttpClientFactory.shared.newCall(request).execute().use { response ->
            if (response.isSuccessful || response.code == 404) {
                store.removeSubscriptionId(registration.connectorToken)
                store.removeRegisteredEndpoint(registration.connectorToken)
                return Result.success(Unit)
            }
            val error = "Failed to replace subscription: ${response.code} ${response.message}"
            Log.e(TAG, error)
            return Result.failure(IOException(error))
        }
    }

    private suspend fun postSubscription(
        serverUrl: String,
        apiKey: String,
        store: PrismPreferences,
        registration: WebPushRegistration,
        vapidPrivateKey: String
    ): Result<String> {
        val json = JSONObject().apply {
            put("appName", registration.appName)
            put("pushEndpoint", registration.webpushUrl)
            put("vapidPrivateKey", vapidPrivateKey)
            registration.p256dh?.let { put("p256dh", it) }
            registration.auth?.let { put("auth", it) }
        }
        val request = Request.Builder()
            .url("$serverUrl/api/v1/webpush/subscriptions")
            .addHeader("Authorization", getAuthHeader(apiKey))
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        debugLog { "Registering app with Prism server: ${registration.appName}" }
        HttpClientFactory.shared.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return try {
                    val subscriptionId = JSONObject(response.body.string()).getString("subscriptionId")
                    store.setSubscriptionId(registration.connectorToken, subscriptionId)
                    store.setRegisteredEndpoint(registration.connectorToken, registration.webpushUrl)
                    Result.success(subscriptionId)
                } catch (e: JSONException) {
                    val error = "Failed to parse registration response: ${e.message}"
                    Log.e(TAG, error)
                    Result.failure(IOException(error))
                }
            }
            val error = "Failed to register app: ${response.code} ${response.message}"
            Log.e(TAG, error)
            return Result.failure(IOException(error))
        }
    }

    fun registerAllApps(context: Context) {
        val config = resolveServerConfig(context)
        if (config == null) {
            debugLog { "Prism server not configured, skipping bulk registration" }
            return
        }
        val store = PrismPreferences(context)

        AppScope.launch {
            try {
                val db = DatabaseFactory.getDb(context)
                val manualApps = listManualApps(context)
                val channelByVapid = db.listChannelIdVapid().associate { (channelId, vapid) -> vapid to channelId }

                debugLog { "Registering ${manualApps.size} manual apps with Prism server" }

                manualApps.forEach { app ->
                    app.endpoint?.let { endpoint ->
                        val appName = app.title ?: app.packageName

                        val channelId = channelByVapid[app.vapidKey]

                        val keyStore = EncryptionKeyStore(context)
                        val keys = channelId?.let { keyStore.getKeys(it) }

                        val storedVapidPrivateKey = store.getVapidPrivateKey(app.connectorToken)
                            ?: getVapidPrivateKeyFromDescription(app.description)

                        registerApp(
                            context,
                            WebPushRegistration(
                                connectorToken = app.connectorToken,
                                appName = appName,
                                webpushUrl = endpoint,
                                vapidPrivateKey = storedVapidPrivateKey,
                                p256dh = keys?.p256dh,
                                auth = keys?.authSecret?.toBase64Url()
                            )
                        )
                    } ?: run {
                        Log.w(TAG, "Skipping app ${app.title} - no endpoint available")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error during bulk registration: ${e.message}", e)
            }
        }
    }

    fun deleteApp(
        context: Context,
        connectorToken: String,
        serverUrl: String? = null,
        apiKey: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val config = resolveServerConfig(context, serverUrl, apiKey)

        if (config == null) {
            debugLog { "Prism server not configured, skipping deletion" }
            return
        }
        val (url, key) = config
        val store = PrismPreferences(context)

        val subscriptionId = store.getSubscriptionId(connectorToken)
            ?: getSubscriptionIdFromDb(context, connectorToken)?.also {
                store.setSubscriptionId(connectorToken, it)
            }

        if (subscriptionId == null) {
            Log.w(TAG, "No subscription ID found for token=${redactIdentifier(connectorToken)}")
            onError("No subscription ID found")
            return
        }

        AppScope.launch {
            try {
                val request = Request.Builder()
                    .url("$url/api/v1/webpush/subscriptions/$subscriptionId")
                    .addHeader("Authorization", getAuthHeader(key))
                    .delete()
                    .build()

                HttpClientFactory.shared.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        store.removeSubscriptionId(connectorToken)
                        store.removeRegisteredEndpoint(connectorToken)
                        withContext(Dispatchers.Main) { onSuccess() }
                    } else {
                        val error = "Failed to delete subscription: ${response.code} ${response.message}"
                        Log.e(TAG, error)
                        withContext(Dispatchers.Main) { onError(error) }
                    }
                }
            } catch (e: IOException) {
                val error = "Error deleting subscription: ${e.message}"
                Log.e(TAG, error, e)
                withContext(Dispatchers.Main) { onError(error) }
            }
        }
    }

    private fun getSubscriptionIdFromDb(context: Context, connectorToken: String): String? {
        val app = DatabaseFactory.getDb(context)
            .listApps()
            .find { it.connectorToken == connectorToken }
            ?: return null

        return DescriptionParser.extractValue(app.description, "sid:")
    }

    private fun getEndpointFromDb(context: Context, connectorToken: String): String? {
        val app = DatabaseFactory.getDb(context)
            .listApps()
            .find { it.connectorToken == connectorToken }
            ?: return null

        return app.endpoint
    }

    private fun getVapidPrivateKeyFromDescription(description: String?): String? =
        DescriptionParser.extractValue(description, DescriptionParser.VAPID_PRIVATE_KEY_PREFIX)

    private fun isValidVapidPrivateKey(privateKey: String): Boolean = try {
        Base64.decode(privateKey, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP).size == 32
    } catch (_: IllegalArgumentException) {
        false
    }

    fun deleteAllApps(
        context: Context,
        serverUrl: String? = null,
        apiKey: String? = null
    ) {
        val config = resolveServerConfig(context, serverUrl, apiKey)

        if (config == null) {
            debugLog { "Prism server not configured, skipping bulk deletion" }
            return
        }
        val (url, key) = config

        AppScope.launch {
            try {
                val manualApps = listManualApps(context)

                debugLog { "Deleting ${manualApps.size} manual apps from Prism server" }

                manualApps.forEach { app ->
                    deleteApp(context, app.connectorToken, url, key)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error during bulk deletion: ${e.message}", e)
            }
        }
    }

    fun testConnection(
        serverUrl: String,
        apiKey: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        AppScope.launch {
            try {
                val healthUrl = "$serverUrl/api/v1/health"
                val request = Request.Builder()
                    .url(healthUrl)
                    .addHeader("Authorization", getAuthHeader(apiKey))
                    .get()
                    .build()

                HttpClientFactory.shared.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        withContext(Dispatchers.Main) { onSuccess() }
                    } else {
                        withContext(Dispatchers.Main) {
                            onError("Connection failed: ${response.code} ${response.message}")
                        }
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    onError("Connection error: ${e.message}")
                }
            }
        }
    }

    fun fetchRegisteredApps(
        context: Context,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        val config = resolveServerConfig(context)
        if (config == null) {
            onSuccess(emptyList())
            return
        }
        val (serverUrl, apiKey) = config

        AppScope.launch {
            try {
                val request = Request.Builder()
                    .url("$serverUrl/api/v1/apps")
                    .addHeader("Authorization", getAuthHeader(apiKey))
                    .get()
                    .build()

                HttpClientFactory.shared.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        val jsonArray = org.json.JSONArray(body)
                        val appNames = mutableListOf<String>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            appNames.add(obj.getString("appName"))
                        }
                        withContext(Dispatchers.Main) { onSuccess(appNames) }
                    } else {
                        withContext(Dispatchers.Main) { onError("Failed to fetch apps: ${response.code}") }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error fetching registered apps: ${e.message}", e)
                withContext(Dispatchers.Main) { onError(e.message ?: "Unknown error") }
            } catch (e: JSONException) {
                Log.e(TAG, "Error fetching registered apps: ${e.message}", e)
                withContext(Dispatchers.Main) { onError(e.message ?: "Unknown error") }
            }
        }
    }

    private fun resolveServerConfig(
        context: Context,
        serverUrl: String? = null,
        apiKey: String? = null
    ): ServerConfig? {
        val resolved = if (!serverUrl.isNullOrBlank() && !apiKey.isNullOrBlank()) {
            serverUrl to apiKey
        } else {
            PrismPreferences(context).getPrismServerConfig()
        } ?: return null

        return ServerConfig(
            serverUrl = resolved.first,
            apiKey = resolved.second
        )
    }

    private fun listManualApps(context: Context) = DatabaseFactory.getDb(context)
        .listApps()
        .filter { DescriptionParser.isManualApp(it.description) }

    private inline fun debugLog(message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message())
        }
    }
}
