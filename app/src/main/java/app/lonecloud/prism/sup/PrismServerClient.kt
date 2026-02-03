package app.lonecloud.prism.sup

import android.content.Context
import android.util.Base64
import android.util.Log
import app.lonecloud.prism.AppStore
import app.lonecloud.prism.DatabaseFactory
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

    private fun getAuthHeader(apiKey: String): String {
        val credentials = ":$apiKey"
        val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        return "Basic $encoded"
    }

    fun registerApp(
        context: Context,
        appName: String,
        upEndpoint: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val store = AppStore(context)
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
                    put("upEndpoint", upEndpoint)
                }

                val request = Request.Builder()
                    .url("$serverUrl/api/webhook/register")
                    .addHeader("Authorization", getAuthHeader(apiKey))
                    .addHeader("Content-Type", "application/json")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                Log.d(TAG, "Registering app with sup server: $appName -> $upEndpoint")

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
        val store = AppStore(context)
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

                Log.d(TAG, "Registering ${manualApps.size} manual apps with sup server")

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

    fun testConnection(
        serverUrl: String,
        apiKey: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(serverUrl)
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
