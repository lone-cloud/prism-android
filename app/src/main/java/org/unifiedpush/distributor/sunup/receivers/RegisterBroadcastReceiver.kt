@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.unifiedpush.distributor.sunup.receivers

import android.content.Context
import org.unifiedpush.android.distributor.receiver.DistributorReceiver
import org.unifiedpush.distributor.sunup.AppStore
import org.unifiedpush.distributor.sunup.Distributor
import org.unifiedpush.distributor.sunup.callback.NetworkCallbackFactory

/**
 * THIS SERVICE IS USED BY OTHER APPS TO REGISTER
 */
class RegisterBroadcastReceiver : DistributorReceiver() {

    override val distributor = Distributor

    override fun isConnected(context: Context): Boolean {
        // We don't have to care about login
        return true
    }

    override fun hasInternet(context: Context): Boolean = NetworkCallbackFactory.hasInternet()

    override fun showToasts(context: Context): Boolean = AppStore(context).showToasts
}
