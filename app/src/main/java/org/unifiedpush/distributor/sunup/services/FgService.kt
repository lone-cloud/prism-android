package org.unifiedpush.distributor.sunup.services

import android.content.Context
import android.util.Log
import androidx.lifecycle.AtomicReference
import org.unifiedpush.distributor.service.ForegroundService
import org.unifiedpush.distributor.service.ForegroundServiceFactory
import org.unifiedpush.distributor.sunup.api.MessageSender
import org.unifiedpush.distributor.sunup.api.ServerConnection
import org.unifiedpush.distributor.sunup.callback.BatteryCallbackFactory
import org.unifiedpush.distributor.sunup.callback.NetworkCallbackFactory
import org.unifiedpush.distributor.sunup.utils.ForegroundNotification
import org.unifiedpush.distributor.sunup.utils.NOTIFICATION_ID_FOREGROUND
import org.unifiedpush.distributor.sunup.utils.TAG

class FgService : ForegroundService() {

    override val networkCallbackFactory = NetworkCallbackFactory
    override val batteryCallbackFactory = BatteryCallbackFactory
    override val registrationCounter = MainRegistrationCounter
    override val workerCompanion = RestartWorker.Companion
    override val staticRef = service

    override fun startForegroundNotification() {
        val notification = ForegroundNotification(this).create()
        startForeground(NOTIFICATION_ID_FOREGROUND, notification)
    }

    override fun shouldAbortNewSync(): Boolean {
        return SourceManager.isRunningWithoutFailure || MessageSender.hasPendingMsgs()
    }

    override fun isConnected(): Boolean {
        return true
    }

    override fun sync(releaseLock: () -> Unit) {
        val ws = ServerConnection(this, releaseLock).start()
        MessageSender.newWs(ws)
    }

    companion object : ForegroundServiceFactory {
        override val service = AtomicReference<ForegroundService?>(null)
        override val serviceClass = FgService::class.java

        override fun startService(context: Context) {
            Log.d(FgService.TAG, "nFails: ${SourceManager.nFails}")
            super.startService(context)
        }
    }
}
