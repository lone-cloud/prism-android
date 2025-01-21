package org.unifiedpush.distributor.sunup.callback

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean
import org.unifiedpush.distributor.callback.CallbackFactory
import org.unifiedpush.distributor.callback.NetworkCallback
import org.unifiedpush.distributor.sunup.services.FailureCounter
import org.unifiedpush.distributor.sunup.services.MainRegistrationCounter
import org.unifiedpush.distributor.sunup.services.RestartWorker

object NetworkCallbackFactory : CallbackFactory<NetworkCallbackFactory.MainNetworkCallback>() {
    class MainNetworkCallback(val context: Context) : NetworkCallback() {
        override val hasInternet = NetworkCallbackFactory.hasInternet
        override val failureCounter = FailureCounter
        override val registrationCounter = MainRegistrationCounter
        override val worker = RestartWorker.Companion
    }

    override fun new(context: Context): MainNetworkCallback {
        return MainNetworkCallback(context)
    }

    /**
     * Default to true
     */
    private val hasInternet = AtomicBoolean(true)

    fun hasInternet(): Boolean {
        return hasInternet.get()
    }
}
