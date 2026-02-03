@file:Suppress("ktlint:standard:no-wildcard-imports")

package app.lonecloud.prism.receivers

import android.content.Context
import app.lonecloud.prism.AppStore
import app.lonecloud.prism.Distributor
import app.lonecloud.prism.callback.NetworkCallbackFactory
import org.unifiedpush.distributor.receiver.DistributorReceiver

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
