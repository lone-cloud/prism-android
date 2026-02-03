@file:Suppress("ktlint:standard:no-wildcard-imports", "WildcardImport")

package app.lonecloud.prism.services

import android.content.Context
import android.util.Log
import androidx.work.*
import app.lonecloud.prism.AppStore
import app.lonecloud.prism.Distributor
import app.lonecloud.prism.api.MessageSender
import app.lonecloud.prism.callback.NetworkCallbackFactory
import app.lonecloud.prism.utils.TAG
import org.unifiedpush.distributor.WorkerCompanion

class RestartWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    /**
     * Restart the service if we have never received an event, or haven't received an event
     * in the expected time
     */
    @Suppress("ReturnCount")
    override fun doWork(): Result {
        // We avoid running twice at the same time
        synchronized(lock) {
            Log.d(TAG, "Working [$id]")
            if (!NetworkCallbackFactory.hasInternet()) {
                Log.d(TAG, "Aborting, no internet.")
                return Result.success()
            }
            if (SourceManager.isRunningWithoutFailure) {
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
            // So, as soon as the user hasn't migrated, we can run
            return !AppStore(context).migrated
        }

        override fun isServiceStarted(context: Context): Boolean = FgService.isServiceStarted()

        override fun enableComponents(context: Context) {
            Distributor.enableComponents(context)
        }

        override fun disableComponents(context: Context) {
            Distributor.disableComponents(context)
        }
    }
}
