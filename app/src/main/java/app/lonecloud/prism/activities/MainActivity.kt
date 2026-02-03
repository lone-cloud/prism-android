package app.lonecloud.prism.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lonecloud.prism.EventBus
import app.lonecloud.prism.activities.ThemeViewModel
import app.lonecloud.prism.activities.ui.App
import app.lonecloud.prism.activities.ui.theme.AppTheme
import app.lonecloud.prism.services.RestartWorker
import app.lonecloud.prism.utils.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var jobs: MutableList<Job> = emptyList<Job>().toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RestartWorker.startPeriodic(this)

        enableEdgeToEdge()

        setContent {
            val factory = ViewModelFactory(this.application)
            val themeViewModel = viewModel<ThemeViewModel>(factory = factory)
            AppTheme(
                dynamicColor = themeViewModel.dynamicColors
            ) {
                App(factory, themeViewModel)
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

    override fun onDestroy() {
        Log.d(TAG, "Destroy")
        jobs.removeAll {
            it.cancel()
            true
        }
        super.onDestroy()
    }
}
