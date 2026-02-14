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
                    put("webpushUrl", webpushUrl)
                    vapidPrivateKey?.let { put("vapidPrivateKey", it) }
                    p256dh?.let { put("p256dh", it) }
                    auth?.let { put("auth", it) }
                }

                val request = Request.Builder()
                    .url("$serverUrl/webpush/app")
                    .addHeader("Authorization", getAuthHeader(apiKey))
                    .addHeader("Content-Type", "application/json")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                Log.d(TAG, "Registering app with Prism server: $appName -> $webpushUrl")

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Successfully registered app: $appName")
                        onSuccess()
                    } else {
                        val error = "Failed to register app: ${response.code} ${response.message}"
                        Log.e(TAG, error)
                        onError(error)
                    }
                }
            } catch (e: IOException) {
                val error = "Error registering app: ${e.message}"
                Log.e(TAG, error, e)
                onError(error)
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
                        registerApp(
                            context,
                            appName,
                            endpoint
                        )
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error during bulk registration: ${e.message}", e)
            }
        }
    }

    fun deleteApp(
        context: Context,
        appName: String,
        serverUrl: String? = null,
        apiKey: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val store = PrismPreferences(context)
        val url = serverUrl ?: store.prismServerUrl
        val key = apiKey ?: store.prismApiKey

        if (url.isNullOrBlank() || key.isNullOrBlank()) {
            Log.d(TAG, "Prism server not configured, skipping deletion")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("$url/webpush/app/$appName")
                    .addHeader("Authorization", getAuthHeader(key))
                    .delete()
                    .build()

                Log.d(TAG, "Deleting app from Prism server: $appName")

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Successfully deleted app: $appName")
                        onSuccess()
                    } else {
                        val error = "Failed to delete app: ${response.code} ${response.message}"
                        Log.e(TAG, error)
                        onError(error)
                    }
                }
            } catch (e: IOException) {
                val error = "Error deleting app: ${e.message}"
                Log.e(TAG, error, e)
                onError(error)
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
                    val appName = app.title ?: app.packageName
                    deleteApp(context, appName, url, key)
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
                val healthUrl = "$serverUrl/api/health"
                val request = Request.Builder()
                    .url(healthUrl)
                    .addHeader("Authorization", getAuthHeader(apiKey))
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Connection failed: ${response.code} ${response.message}")
                    }
                }
            } catch (e: IOException) {
                onError("Connection error: ${e.message}")
            }
        }
    }
}
