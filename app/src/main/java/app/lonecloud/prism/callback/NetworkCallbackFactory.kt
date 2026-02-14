/*
 * Copyright (C) 2024 p1gp1g
 * Modified by lone-cloud under AGPL v3.0
 *
 * Original work: https://codeberg.org/Sunup/android
 * Licensed under Apache License 2.0
 */

package app.lonecloud.prism.callback

import android.content.Context
import app.lonecloud.prism.services.MainRegistrationCounter
import app.lonecloud.prism.services.RestartWorker
import app.lonecloud.prism.services.SourceManager
import org.unifiedpush.android.distributor.callback.CallbackFactory
import org.unifiedpush.android.distributor.callback.NetworkCallback

object NetworkCallbackFactory : CallbackFactory<NetworkCallbackFactory.MainNetworkCallback>() {
    class MainNetworkCallback(val context: Context) : NetworkCallback() {
        override val sourceManager = SourceManager
        override val registrationCounter = MainRegistrationCounter
        override val worker = RestartWorker.Companion
    }

    override fun new(context: Context): MainNetworkCallback = MainNetworkCallback(context)

    fun hasInternet(): Boolean = this.instance?.hasInternet ?: true
}
