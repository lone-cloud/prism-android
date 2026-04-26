/*
 * Copyright (C) 2024 p1gp1g
 * Modified by lone-cloud under AGPL v3.0
 *
 * Original work: https://codeberg.org/Sunup/android
 * Licensed under Apache License 2.0
 */

@file:Suppress("ktlint:standard:no-wildcard-imports")

package app.lonecloud.prism.receivers

import android.content.Context
import android.content.Intent
import app.lonecloud.prism.Distributor
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.callback.NetworkCallbackFactory
import org.unifiedpush.android.distributor.receiver.DistributorReceiver

class RegisterBroadcastReceiver : DistributorReceiver() {

    private var suppressToastForManual = false

    override val distributor = Distributor

    override fun isConnected(context: Context): Boolean = true

    override fun hasInternet(context: Context): Boolean = NetworkCallbackFactory.hasInternet()

    override fun showToasts(context: Context): Boolean = !suppressToastForManual && PrismPreferences(context).showToasts

    override fun onReceive(rContext: Context, intent: Intent?) {
        val token = intent?.getStringExtra("token")
        suppressToastForManual = token?.startsWith("manual_app_") == true
        try {
            super.onReceive(rContext, intent)
        } finally {
            suppressToastForManual = false
        }
    }
}
