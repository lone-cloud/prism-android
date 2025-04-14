package org.unifiedpush.distributor.sunup.callback

import android.content.Context
import org.unifiedpush.distributor.callback.CallbackFactory
import org.unifiedpush.distributor.callback.NetworkCallback
import org.unifiedpush.distributor.sunup.services.FailureCounter
import org.unifiedpush.distributor.sunup.services.MainRegistrationCounter
import org.unifiedpush.distributor.sunup.services.RestartWorker

object NetworkCallbackFactory : CallbackFactory<NetworkCallbackFactory.MainNetworkCallback>() {
    class MainNetworkCallback(val context: Context) : NetworkCallback() {
        override val failureCounter = FailureCounter
        override val registrationCounter = MainRegistrationCounter
        override val worker = RestartWorker.Companion
    }

    override fun new(context: Context): MainNetworkCallback {
        return MainNetworkCallback(context)
    }

    fun hasInternet(): Boolean {
        return this.instance?.hasInternet() ?: true
    }
}
