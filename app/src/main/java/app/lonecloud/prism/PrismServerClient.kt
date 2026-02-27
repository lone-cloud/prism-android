package app.lonecloud.prism

import android.content.Context
import android.util.Base64
import android.util.Log
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.utils.DescriptionParser
import app.lonecloud.prism.utils.HttpClientFactory
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
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
    private const val VAPID_PRIVATE_DESC_PREFIX = "vp:"

    private fun getAuthHeader(apiKey: String): String = "Bearer $apiKey"

    fun registerApp(
        context: Context,
        connectorToken: String,
        appName: String,
        webpushUrl: String,
        vapidPrivateKey: String? = null,
        p256dh: String? = null,
        auth: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val config = PrismPreferences(context).getPrismServerConfig()
        if (config == null) {
            val error = "Prism server not configured"
            Log.d(TAG, "$error, skipping registration")
            onError(error)
            return
        }
        val (serverUrl, apiKey) = config
        val store = PrismPreferences(context)
        val existingSubscriptionId = store.getSubscriptionId(connectorToken)
            ?: getSubscriptionIdFromDb(context, connectorToken)?.also {
                store.setSubscriptionId(connectorToken, it)
                Log.d(TAG, "registerApp: restored subscriptionId from description for $connectorToken")
            }
        val knownEndpoint = store.getRegisteredEndpoint(connectorToken)
            ?: getEndpointFromDb(context, connectorToken)?.also {
                store.setRegisteredEndpoint(connectorToken, it)
                Log.d(TAG, "registerApp: restored endpoint from db for $connectorToken")
            }

        Log.d(
            TAG,
            "registerApp decision: token=$connectorToken existingSub=${!existingSubscriptionId.isNullOrBlank()} endpointMatch=${knownEndpoint == webpushUrl}"
        )

        if (!existingSubscriptionId.isNullOrBlank() && knownEndpoint == webpushUrl) {
            Log.d(TAG, "registerApp: skipping duplicate create for $appName (endpoint unchanged)")
            onSuccess()
            return
        }

        if (vapidPrivateKey.isNullOrBlank() || !isValidVapidPrivateKey(vapidPrivateKey)) {
            val error = "Invalid VAPID private key for $appName. Delete and re-register the app."
            Log.e(TAG, error)
            onError(error)
            return
        }
        val validatedVapidPrivateKey = requireNotNull(vapidPrivateKey)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (
                    !existingSubscriptionId.isNullOrBlank() &&
                    !knownEndpoint.isNullOrBlank() &&
                    knownEndpoint != webpushUrl
                ) {
                    val deleteRequest = Request.Builder()
                        .url("$serverUrl/api/v1/webpush/subscriptions/$existingSubscriptionId")
                        .addHeader("Authorization", getAuthHeader(apiKey))
                        .delete()
                        .build()

                    Log.w(
                        TAG,
                        "registerApp: endpoint changed for $appName, replacing subscription $existingSubscriptionId"
                    )

                    HttpClientFactory.shared.newCall(deleteRequest).execute().use { deleteResponse ->
                        if (deleteResponse.isSuccessful || deleteResponse.code == 404) {
                            store.removeSubscriptionId(connectorToken)
                            store.removeRegisteredEndpoint(connectorToken)
                        } else {
                            val body = deleteResponse.body.string()
                            val error = "Failed to replace subscription: ${deleteResponse.code} ${deleteResponse.message}"
                            Log.e(TAG, "$error - Response: $body")
                            withContext(Dispatchers.Main) { onError(error) }
                            return@launch
                        }
                    }
                }

                val json = JSONObject().apply {
                    put("appName", appName)
                    put("pushEndpoint", webpushUrl)
                    put("vapidPrivateKey", validatedVapidPrivateKey)
                    p256dh?.let { put("p256dh", it) }
                    auth?.let { put("auth", it) }
                }

                val url = "$serverUrl/api/v1/webpush/subscriptions"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", getAuthHeader(apiKey))
                    .addHeader("Content-Type", "application/json")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                Log.d(TAG, "Registering app with Prism server: $appName")

                HttpClientFactory.shared.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body.string()

                        try {
                            val responseJson = JSONObject(responseBody)
                            val subscriptionId = responseJson.getString("subscriptionId")

                            store.setSubscriptionId(connectorToken, subscriptionId)
                            store.setRegisteredEndpoint(connectorToken, webpushUrl)

                            Log.d(TAG, "Successfully registered app: $appName (ID: $subscriptionId)")
                            withContext(Dispatchers.Main) { onSuccess() }
                        } catch (e: JSONException) {
                            val error = "Failed to parse registration response: ${e.message}"
                            Log.e(TAG, error)
                            Log.e(TAG, "Response body: $responseBody")
                            withContext(Dispatchers.Main) { onError(error) }
                        }
                    } else {
                        val responseBody = response.body.string()
                        val error = "Failed to register app: ${response.code} ${response.message}"
                        Log.e(TAG, "$error - Response: $responseBody")
                        withContext(Dispatchers.Main) { onError(error) }
                    }
                }
            } catch (e: IOException) {
                val error = "Error registering app: ${e.message}"
                Log.e(TAG, error, e)
                withContext(Dispatchers.Main) { onError(error) }
            }
        }
    }

    fun registerAllApps(context: Context) {
        val config = PrismPreferences(context).getPrismServerConfig()
        if (config == null) {
            Log.d(TAG, "Prism server not configured, skipping bulk registration")
            return
        }
        val store = PrismPreferences(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseFactory.getDb(context)
                val apps = db.listApps()

                val manualApps = apps.filter { DescriptionParser.isManualApp(it.description) }

                Log.d(TAG, "Registering ${manualApps.size} manual apps with Prism server")

                manualApps.forEach { app ->
                    app.endpoint?.let { endpoint ->
                        val appName = app.title ?: app.packageName

                        val channelId = db.listChannelIdVapid()
                            .find { (_, vapid) -> vapid == app.vapidKey }
                            ?.first

                        val keyStore = EncryptionKeyStore(context)
                        val keys = channelId?.let { keyStore.getKeys(it) }

                        val storedVapidPrivateKey = store.getVapidPrivateKey(app.connectorToken)
                            ?: getVapidPrivateKeyFromDescription(app.description)

                        registerApp(
                            context,
                            app.connectorToken,
                            appName,
                            endpoint,
                            vapidPrivateKey = storedVapidPrivateKey,
                            p256dh = keys?.third,
                            auth = keys?.second?.let { authBytes ->
                                android.util.Base64.encodeToString(
                                    authBytes,
                                    android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
                                )
                            }
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
        Log.d(TAG, "deleteApp called for connectorToken: $connectorToken")

        val config = if (serverUrl != null && apiKey != null) {
            serverUrl to apiKey
        } else {
            PrismPreferences(context).getPrismServerConfig()
        }

        if (config == null) {
            Log.d(TAG, "Prism server not configured, skipping deletion")
            return
        }
        val (url, key) = config
        val store = PrismPreferences(context)

        val subscriptionId = store.getSubscriptionId(connectorToken)
            ?: getSubscriptionIdFromDb(context, connectorToken)?.also {
                store.setSubscriptionId(connectorToken, it)
            }
        Log.d(TAG, "Retrieved subscriptionId: $subscriptionId for token: $connectorToken")

        if (subscriptionId == null) {
            Log.w(TAG, "No subscription ID found for token: $connectorToken")
            onError("No subscription ID found")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("$url/api/v1/webpush/subscriptions/$subscriptionId")
                    .addHeader("Authorization", getAuthHeader(key))
                    .delete()
                    .build()

                Log.d(TAG, "Deleting subscription from Prism server: $subscriptionId")

                HttpClientFactory.shared.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        store.removeSubscriptionId(connectorToken)
                        store.removeRegisteredEndpoint(connectorToken)
                        Log.d(TAG, "Successfully deleted subscription: $subscriptionId")
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
        DescriptionParser.extractValue(description, VAPID_PRIVATE_DESC_PREFIX)

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
        val config = if (serverUrl != null && apiKey != null) {
            serverUrl to apiKey
        } else {
            PrismPreferences(context).getPrismServerConfig()
        }

        if (config == null) {
            Log.d(TAG, "Prism server not configured, skipping bulk deletion")
            return
        }
        val (url, key) = config

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseFactory.getDb(context)
                val apps = db.listApps()

                val manualApps = apps.filter { DescriptionParser.isManualApp(it.description) }

                Log.d(TAG, "Deleting ${manualApps.size} manual apps from Prism server")

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
        CoroutineScope(Dispatchers.IO).launch {
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
        val config = PrismPreferences(context).getPrismServerConfig()
        if (config == null) {
            onSuccess(emptyList())
            return
        }
        val (serverUrl, apiKey) = config

        CoroutineScope(Dispatchers.IO).launch {
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
}
