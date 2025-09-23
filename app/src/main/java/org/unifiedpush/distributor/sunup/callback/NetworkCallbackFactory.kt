package org.unifiedpush.distributor.sunup.callback

import android.content.Context
import org.unifiedpush.distributor.callback.CallbackFactory
import org.unifiedpush.distributor.callback.NetworkCallback
import org.unifiedpush.distributor.sunup.services.MainRegistrationCounter
import org.unifiedpush.distributor.sunup.services.RestartWorker
import org.unifiedpush.distributor.sunup.services.SourceManager

object NetworkCallbackFactory : CallbackFactory<NetworkCallbackFactory.MainNetworkCallback>() {
    class MainNetworkCallback(val context: Context) : NetworkCallback() {
        override val sourceManager = SourceManager
        override val registrationCounter = MainRegistrationCounter
        override val worker = RestartWorker.Companion
    }

    override fun new(context: Context): MainNetworkCallback = MainNetworkCallback(context)

    fun hasInternet(): Boolean = this.instance?.hasInternet() ?: true
}
