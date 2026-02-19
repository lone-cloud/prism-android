package app.lonecloud.prism.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.lonecloud.prism.BuildConfig
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.api.ApiUrlCandidate
import app.lonecloud.prism.services.RestartWorker

class PrismConfigReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = PrismPreferences(context)

        when (intent.action) {
            ACTION_SET_PRISM_SERVER_URL -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: ""
                store.prismServerUrl = url
            }
            ACTION_SET_PRISM_API_KEY -> {
                val apiKey = intent.getStringExtra(EXTRA_API_KEY) ?: ""
                store.prismApiKey = apiKey
            }
            ACTION_SET_PUSH_SERVICE_URL -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: ""
                if (url.isBlank()) {
                    store.apiUrl = BuildConfig.DEFAULT_API_URL
                    RestartWorker.run(context, delay = 0)
                } else {
                    ApiUrlCandidate.test(context, url)
                }
            }
        }
    }

    companion object {
        const val ACTION_SET_PRISM_SERVER_URL = "app.lonecloud.prism.SET_PRISM_SERVER_URL"
        const val ACTION_SET_PRISM_API_KEY = "app.lonecloud.prism.SET_PRISM_API_KEY"
        const val ACTION_SET_PUSH_SERVICE_URL = "app.lonecloud.prism.SET_PUSH_SERVICE_URL"
        const val EXTRA_URL = "url"
        const val EXTRA_API_KEY = "api_key"
    }
}
