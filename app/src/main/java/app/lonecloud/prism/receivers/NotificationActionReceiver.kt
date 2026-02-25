package app.lonecloud.prism.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.utils.HttpClientFactory
import app.lonecloud.prism.utils.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        val notificationTag = intent.getStringExtra("notificationTag") ?: ""
        val notificationId = intent.getIntExtra("notificationId", -1)

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

        if (notificationTag.isNotEmpty() && notificationId != -1) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notificationTag, notificationId)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                executeAction(context, actionEndpoint, actionMethod, data)
            } catch (e: Exception) {
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
        val fullUrl = if (endpoint.startsWith("http")) {
            endpoint
        } else {
            serverUrl.trimEnd('/') + "/" + endpoint.trimStart('/')
        }

        val jsonBody = JSONObject(data as Map<*, *>).toString()
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(fullUrl)
            .method(method, if (method == "GET" || method == "HEAD") null else requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d(TAG, "Executing action: $method $fullUrl with data: $jsonBody")

        HttpClientFactory.action.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                Log.d(TAG, "Action executed successfully: ${response.code}")
            } else {
                Log.e(TAG, "Action failed: ${response.code} ${response.message}")
            }
        }
    }
}
