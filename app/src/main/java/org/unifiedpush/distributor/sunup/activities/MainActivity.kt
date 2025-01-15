package org.unifiedpush.distributor.sunup.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.unifiedpush.distributor.sunup.EventBus
import org.unifiedpush.distributor.sunup.Migrations
import org.unifiedpush.distributor.sunup.activities.ui.MainUi
import org.unifiedpush.distributor.sunup.activities.ui.theme.AppTheme
import org.unifiedpush.distributor.sunup.services.RestartWorker
import org.unifiedpush.distributor.sunup.utils.TAG

class MainActivity : ComponentActivity() {
    private var viewModel: MainViewModel? = null
    private var jobs: MutableList<Job> = emptyList<Job>().toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RestartWorker.startPeriodic(this)
        Migrations(this).run()

        setContent {
            val viewModel =
                viewModel {
                    MainViewModel(this@MainActivity)
                }.also {
                    viewModel = it
                }
            AppTheme {
                MainUi(viewModel)
            }
            subscribeActions()
        }
    }

    private fun subscribeActions() {
        Log.d(TAG, "Subscribing to actions")
        jobs += CoroutineScope(Dispatchers.IO).launch {
            EventBus.subscribe<AppAction> { it.handle(this@MainActivity) }
        }
        jobs += CoroutineScope(Dispatchers.IO).launch {
            EventBus.subscribe<UiAction> {
                it.handle { type ->
                    when (type) {
                        UiAction.Action.RefreshRegistrations -> viewModel?.refreshRegistrations(
                            this@MainActivity
                        )
                        UiAction.Action.RefreshApiUrl -> viewModel?.refreshApiUrl(
                            this@MainActivity
                        )
                    }
                }
            }
        }
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
