package app.lonecloud.prism.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.lonecloud.prism.AppStore

class PrismConfigReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = AppStore(context)

        when (intent.action) {
            ACTION_SET_PRISM_SERVER_URL -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: ""
                store.prismServerUrl = url
            }
            ACTION_SET_PRISM_API_KEY -> {
                val apiKey = intent.getStringExtra(EXTRA_API_KEY) ?: ""
                store.prismApiKey = apiKey
            }
        }
    }

    companion object {
        const val ACTION_SET_PRISM_SERVER_URL = "app.lonecloud.prism.SET_PRISM_SERVER_URL"
        const val ACTION_SET_PRISM_API_KEY = "app.lonecloud.prism.SET_PRISM_API_KEY"
        const val EXTRA_URL = "url"
        const val EXTRA_API_KEY = "api_key"
    }
}
