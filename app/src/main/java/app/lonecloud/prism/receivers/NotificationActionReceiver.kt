package app.lonecloud.prism.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.lonecloud.prism.AppScope
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.utils.HttpClientFactory
import app.lonecloud.prism.utils.ManualAppNotifications
import app.lonecloud.prism.utils.TAG
import java.io.IOException
import java.net.URI
import java.util.Locale
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val channelID = intent.getStringExtra("channelID") ?: return
        val actionID = intent.getStringExtra("actionID") ?: return
        val actionLabel = intent.getStringExtra("actionLabel") ?: ""
        val actionEndpoint = intent.getStringExtra("actionEndpoint") ?: return
        val actionMethod = intent.getStringExtra("actionMethod") ?: "POST"
        val connectorToken = intent.getStringExtra("connectorToken")
        val notificationTag = intent.getStringExtra("notificationTag") ?: ""

        val data = mutableMapOf<String, String>()
        intent.extras?.keySet()?.forEach { key ->
            if (key.startsWith("data_")) {
                val dataKey = key.substring(5)
                val value = intent.getStringExtra(key)
                if (value != null) {
                    data[dataKey] = value
                }
            }
        }

        Log.d(TAG, "Notification action triggered: $actionLabel ($actionID) for channel $channelID")

        if (notificationTag.isNotEmpty()) {
            ManualAppNotifications.dismissNotification(context, notificationTag, connectorToken)
        }

        val pendingResult = goAsync()
        AppScope.launch {
            try {
                executeAction(context, actionEndpoint, actionMethod, data)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to execute notification action: ${e.message}", e)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to execute notification action: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun executeAction(
        context: Context,
        endpoint: String,
        method: String,
        data: Map<String, String>
    ) {
        val config = PrismPreferences(context).getPrismServerConfig()
        if (config == null) {
            Log.e(TAG, "Prism server not configured")
            return
        }
        val (serverUrl, apiKey) = config
        val normalizedMethod = method.uppercase(Locale.US)
        require(normalizedMethod in SUPPORTED_METHODS) { "Unsupported action method: $method" }

        val fullUrl = requireNotNull(resolveAndValidateActionUrl(serverUrl, endpoint)) {
            "Action endpoint must match configured Prism server origin"
        }

        val jsonBody = JSONObject(data as Map<*, *>).toString()
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(fullUrl)
            .method(normalizedMethod, if (normalizedMethod == "GET" || normalizedMethod == "HEAD") null else requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d(TAG, "Executing action: $normalizedMethod ${request.url.encodedPath}")

        HttpClientFactory.action.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                Log.d(TAG, "Action executed successfully: ${response.code}")
            } else {
                Log.e(TAG, "Action failed: ${response.code} ${response.message}")
            }
        }
    }

    private fun resolveAndValidateActionUrl(serverUrl: String, endpoint: String): String? {
        val baseUri = parseHttpUri(serverUrl) ?: return null
        val resolvedUri = parseHttpUri(baseUri.resolve(endpoint.trim()).toString()) ?: return null
        if (!isSameOrigin(baseUri, resolvedUri)) {
            return null
        }
        return resolvedUri.toString()
    }

    private fun parseHttpUri(value: String): URI? {
        val uri = try {
            URI(value.trim())
        } catch (_: IllegalArgumentException) {
            return null
        }
        val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
        if (scheme != "http" && scheme != "https") {
            return null
        }
        if (uri.host.isNullOrBlank()) {
            return null
        }
        return uri
    }

    private fun isSameOrigin(base: URI, candidate: URI): Boolean {
        val baseScheme = base.scheme.lowercase(Locale.US)
        val candidateScheme = candidate.scheme.lowercase(Locale.US)
        return baseScheme == candidateScheme &&
            base.host.equals(candidate.host, ignoreCase = true) &&
            normalizedPort(base) == normalizedPort(candidate)
    }

    private fun normalizedPort(uri: URI): Int = when {
        uri.port != -1 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }

    companion object {
        private val SUPPORTED_METHODS = setOf("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE")
    }
}
