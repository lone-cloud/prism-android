package app.lonecloud.prism.api

import android.content.Context
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.services.RestartWorker
import app.lonecloud.prism.services.SourceManager
import java.util.concurrent.atomic.AtomicReference

/**
 * Candidate for a new API url
 *
 * ```
 * [New] -> [Testing] -> [None]
 *      |-> [New]    |-> [New]
 * ```
 */
sealed class ApiUrlCandidate {
    private data object None : ApiUrlCandidate()
    private data class New(val url: String) : ApiUrlCandidate()
    private data class Testing(val url: String) : ApiUrlCandidate()

    companion object {
        private const val FAKE_TOKEN = "fake_token"
        private var instance = AtomicReference<ApiUrlCandidate>(None)

        /**
         * Set [New] candidate for [url]
         *
         * 2 strategies to test:
         * - If there is no registrations, add one and it will try to connect
         * - Else, fail once and restart
         */
        fun test(context: Context, url: String) {
            instance.set(New(url))
            DatabaseFactory.getDb(context).run {
                if (countApps() == 0) {
                    registerApp(
                        context.packageName,
                        FAKE_TOKEN,
                        FAKE_TOKEN,
                        null,
                        null,
                        null
                    )
                } else {
                    SourceManager.setFailOnce()
                    RestartWorker.run(context, delay = 1_000)
                }
            }
        }

        /**
         * Returns the url to test if it is a [New] candidate,
         * and change to a [Testing] candidate
         */
        fun getTest(): String? {
            val candidate = instance.get()
            return if (candidate is New) {
                instance.set(Testing(candidate.url))
                candidate.url
            } else {
                null
            }
        }

        /**
         * Returns the url testing if it is a [Testing] candidate
         * and change to a [None] candidate
         */
        fun finish(context: Context): String? {
            DatabaseFactory.getDb(context).unregisterApp(FAKE_TOKEN)
            val candidate = instance.get()
            if (candidate is Testing) {
                instance.set(None)
                return candidate.url
            }
            return null
        }
    }
}
