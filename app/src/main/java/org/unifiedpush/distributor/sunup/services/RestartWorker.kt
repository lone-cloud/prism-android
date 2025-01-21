@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.unifiedpush.distributor.sunup.services

import android.content.Context
import android.util.Log
import androidx.work.*
import org.unifiedpush.distributor.WorkerCompanion
import org.unifiedpush.distributor.sunup.api.MessageSender
import org.unifiedpush.distributor.sunup.callback.NetworkCallbackFactory
import org.unifiedpush.distributor.sunup.utils.TAG

class RestartWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    /**
     * Restart the service if we have never received an event, or haven't received an event
     * in the expected time
     */
    override fun doWork(): Result {
        // We avoid running twice at the same time
        synchronized(lock) {
            Log.d(TAG, "Working [$id]")
            if (!NetworkCallbackFactory.hasInternet()) {
                Log.d(TAG, "Aborting, no internet.")
                return Result.success()
            }
            if (FailureCounter.isRunningWithoutFailure) {
                Log.d(TAG, "Running without failure")
                // We send a ping, if it fails it will restart this worker, and wont
                // pass this check
                MessageSender.ping(applicationContext)
                return Result.success()
            }
            Log.d(TAG, "Restarting")
            FgService.startService(applicationContext)
            return Result.success()
        }
    }

    companion object : WorkerCompanion(RestartWorker::class.java) {
        private val lock = Object()
        override fun canRun(context: Context): Boolean {
            // We don't have any credential requirement, if we don't have
            // a uaid yet, it will be created during the initial sync
            return true
        }
    }
}
