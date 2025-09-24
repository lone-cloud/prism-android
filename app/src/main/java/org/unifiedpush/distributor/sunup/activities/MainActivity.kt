package org.unifiedpush.distributor.sunup.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.unifiedpush.distributor.sunup.EventBus
import org.unifiedpush.distributor.sunup.Migrations
import org.unifiedpush.distributor.sunup.activities.ui.App
import org.unifiedpush.distributor.sunup.activities.ui.theme.AppTheme
import org.unifiedpush.distributor.sunup.services.RestartWorker
import org.unifiedpush.distributor.sunup.utils.TAG

class MainActivity : ComponentActivity() {
    private var jobs: MutableList<Job> = emptyList<Job>().toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RestartWorker.startPeriodic(this)
        Migrations(this).run()

        enableEdgeToEdge()

        setContent {
            AppTheme {
                App(ViewModelFactory(this.application))
            }
            subscribeActions()
        }
    }

    private fun subscribeActions() {
        Log.d(TAG, "Subscribing to actions")
        jobs += CoroutineScope(Dispatchers.IO).launch {
            EventBus.subscribe<AppAction> { it.handle(this@MainActivity) }
        }
    }

    override fun onResume() {
        Log.d(TAG, "Resumed")
        UiAction.publish(UiAction.Action.RefreshRegistrations)
        super.onResume()
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroy")
        jobs.removeAll {
            it.cancel()
            true
        }
        super.onDestroy()
    }
}
