package org.unifiedpush.distributor.sunup.api

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
        private var instance = AtomicReference<ApiUrlCandidate>(None)

        /**
         * Set [New] candidate for [url]
         */
        fun test(url: String) {
            instance.set(New(url))
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
        fun finish(): String? {
            val candidate = instance.get()
            if (candidate is Testing) {
                instance.set(None)
                return candidate.url
            }
            return null
        }
    }
}
