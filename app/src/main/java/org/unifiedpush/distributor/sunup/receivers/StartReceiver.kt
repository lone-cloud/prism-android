package org.unifiedpush.distributor.sunup.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.unifiedpush.distributor.sunup.services.RestartWorker

class StartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            RestartWorker.startPeriodic(context)
        }
    }
}
