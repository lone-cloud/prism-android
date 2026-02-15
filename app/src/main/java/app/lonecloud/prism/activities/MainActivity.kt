/*
 * Copyright (C) 2024 p1gp1g
 * Modified by lone-cloud under AGPL v3.0
 *
 * Original work: https://codeberg.org/Sunup/android
 * Licensed under Apache License 2.0
 */

package app.lonecloud.prism.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lonecloud.prism.activities.ui.App
import app.lonecloud.prism.activities.ui.theme.AppTheme
import app.lonecloud.prism.utils.TAG
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.unifiedpush.android.distributor.ipc.InternalMessenger

class MainActivity : ComponentActivity() {
    private lateinit var messenger: InternalMessenger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        messenger = InternalMessenger(this)
        jobs.removeAll {
            Log.d(TAG, "Cancelling exitProcess job")
            it.cancel()
            true
        }

        enableEdgeToEdge()

        setContent {
            val factory = ViewModelFactory(this.application, messenger)
            val themeViewModel = viewModel<ThemeViewModel>(factory = factory)
            AppTheme(
                dynamicColor = themeViewModel.dynamicColors
            ) {
                App(factory, themeViewModel)
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroy")
        super.onDestroy()
        jobs += CoroutineScope(Dispatchers.Main + Job()).launch {
            delay(10_000)
            exitProcess(0)
        }
    }

    companion object {
        val jobs = emptyList<Job>().toMutableList()
    }
}
