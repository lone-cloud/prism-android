/*
 * Copyright (C) 2024 p1gp1g
 * Modified by lone-cloud under AGPL v3.0
 *
 * Original work: https://codeberg.org/Sunup/android
 * Licensed under Apache License 2.0
 */

package app.lonecloud.prism.services

import android.content.Context
import android.content.Intent
import android.util.Log
import app.lonecloud.prism.api.MessageSender
import app.lonecloud.prism.api.ServerConnection
import app.lonecloud.prism.callback.BatteryCallbackFactory
import app.lonecloud.prism.callback.NetworkCallbackFactory
import app.lonecloud.prism.utils.ForegroundNotification
import app.lonecloud.prism.utils.NOTIFICATION_ID_FOREGROUND
import app.lonecloud.prism.utils.TAG
import java.util.concurrent.atomic.AtomicReference
import org.unifiedpush.android.distributor.service.ForegroundService
import org.unifiedpush.android.distributor.service.ForegroundServiceFactory

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

    // Force a fresh DB read before the base class checks oneOrMore() to avoid stale
    // in-memory count from a previous start where the DB was empty (e.g. fresh install).
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        registrationCounter.refresh(this)
        return super.onStartCommand(intent, flags, startId)
    }

    // The actual connection check is delegated to shouldAbortNewSync() via SourceManager.
    // The base class splits these concerns: isConnected() gates reconnect scheduling,
    // while shouldAbortNewSync() gates whether a new sync attempt should proceed.
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
