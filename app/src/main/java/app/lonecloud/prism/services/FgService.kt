package app.lonecloud.prism.services

import android.content.Context
import android.util.Log
import app.lonecloud.prism.api.MessageSender
import app.lonecloud.prism.api.ServerConnection
import app.lonecloud.prism.callback.BatteryCallbackFactory
import app.lonecloud.prism.callback.NetworkCallbackFactory
import app.lonecloud.prism.utils.ForegroundNotification
import app.lonecloud.prism.utils.NOTIFICATION_ID_FOREGROUND
import app.lonecloud.prism.utils.TAG
import java.util.concurrent.atomic.AtomicReference
import org.unifiedpush.distributor.service.ForegroundService
import org.unifiedpush.distributor.service.ForegroundServiceFactory

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

    override fun shouldAbortNewSync(): Boolean = SourceManager.isRunningWithoutFailure

    override fun isConnected(): Boolean = true

    override fun sync(releaseLock: () -> Unit) {
        val ws = ServerConnection(this, releaseLock).start()
        MessageSender.newWs(ws)
    }

    override fun destroyServiceResources() {
        ServerConnection.destroy()
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
