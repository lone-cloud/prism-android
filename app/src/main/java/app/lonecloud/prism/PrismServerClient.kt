package app.lonecloud.prism

import android.content.Context
import android.util.Log
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.PrismPreferences
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object PrismServerClient {
    private const val TAG = "PrismServerClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

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
        val store = PrismPreferences(context)
        val serverUrl = store.prismServerUrl
        val apiKey = store.prismApiKey

        if (serverUrl.isNullOrBlank() || apiKey.isNullOrBlank()) {
            Log.d(TAG, "Prism server not configured, skipping registration")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("appName", appName)
                    put("pushEndpoint", webpushUrl)
                    vapidPrivateKey?.let { put("vapidPrivateKey", it) }
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

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body.string()

                        try {
                            val responseJson = JSONObject(responseBody)
                            val subscriptionId = responseJson.getString("subscriptionId").toLong()

                            store.setSubscriptionId(connectorToken, subscriptionId)

                            Log.d(TAG, "Successfully registered app: $appName (ID: $subscriptionId)")
                            withContext(Dispatchers.Main) { onSuccess() }
                        } catch (e: Exception) {
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
        val store = PrismPreferences(context)
        val serverUrl = store.prismServerUrl
        val apiKey = store.prismApiKey

        if (serverUrl.isNullOrBlank() || apiKey.isNullOrBlank()) {
            Log.d(TAG, "Prism server not configured, skipping bulk registration")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseFactory.getDb(context)
                val apps = db.listApps()

                val manualApps = apps.filter { it.description?.startsWith("target:") == true }

                Log.d(TAG, "Registering ${manualApps.size} manual apps with Prism server")

                manualApps.forEach { app ->
                    app.endpoint?.let { endpoint ->
                        val appName = app.title ?: app.packageName

                        val channelId = db.listChannelIdVapid()
                            .find { (_, vapid) -> vapid == app.vapidKey }
                            ?.first

                        val keyStore = EncryptionKeyStore(context)
                        val keys = channelId?.let { keyStore.getKeys(it) }

                        registerApp(
                            context,
                            app.connectorToken,
                            appName,
                            endpoint,
                            vapidPrivateKey = app.vapidKey,
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
        val store = PrismPreferences(context)
        val url = serverUrl ?: store.prismServerUrl
        val key = apiKey ?: store.prismApiKey

        Log.d(TAG, "deleteApp called for connectorToken: $connectorToken")

        if (url.isNullOrBlank() || key.isNullOrBlank()) {
            Log.d(TAG, "Prism server not configured, skipping deletion")
            return
        }

        val subscriptionId = store.getSubscriptionId(connectorToken)
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

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        store.removeSubscriptionId(connectorToken)
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

    fun deleteAllApps(
        context: Context,
        serverUrl: String? = null,
        apiKey: String? = null
    ) {
        val store = PrismPreferences(context)
        val url = serverUrl ?: store.prismServerUrl
        val key = apiKey ?: store.prismApiKey

        if (url.isNullOrBlank() || key.isNullOrBlank()) {
            Log.d(TAG, "Prism server not configured, skipping bulk deletion")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseFactory.getDb(context)
                val apps = db.listApps()

                val manualApps = apps.filter { it.description?.startsWith("target:") == true }

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

                client.newCall(request).execute().use { response ->
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
        val store = PrismPreferences(context)
        val serverUrl = store.prismServerUrl
        val apiKey = store.prismApiKey

        if (serverUrl.isNullOrBlank() || apiKey.isNullOrBlank()) {
            onSuccess(emptyList())
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("$serverUrl/api/v1/apps")
                    .addHeader("Authorization", getAuthHeader(apiKey))
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
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
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching registered apps: ${e.message}", e)
                withContext(Dispatchers.Main) { onError(e.message ?: "Unknown error") }
            }
        }
    }
}
