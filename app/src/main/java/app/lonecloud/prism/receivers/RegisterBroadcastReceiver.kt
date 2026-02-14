@file:Suppress("ktlint:standard:no-wildcard-imports")

package app.lonecloud.prism.receivers

import android.content.Context
import app.lonecloud.prism.Distributor
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.callback.NetworkCallbackFactory
import org.unifiedpush.android.distributor.receiver.DistributorReceiver

class RegisterBroadcastReceiver : DistributorReceiver() {

    override val distributor = Distributor

    override fun isConnected(context: Context): Boolean = true

    override fun hasInternet(context: Context): Boolean = NetworkCallbackFactory.hasInternet()

    override fun showToasts(context: Context): Boolean = PrismPreferences(context).showToasts
}
