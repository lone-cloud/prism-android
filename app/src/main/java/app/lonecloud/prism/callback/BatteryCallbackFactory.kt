package app.lonecloud.prism.callback

import android.content.Context
import org.unifiedpush.distributor.callback.BatteryCallback
import org.unifiedpush.distributor.callback.CallbackFactory

/**
 * Battery callback - disabled since URGENCY feature is not supported by the Mozilla server
 */
object BatteryCallbackFactory : CallbackFactory<BatteryCallbackFactory.MainBatteryCallback>() {

    class MainBatteryCallback : BatteryCallback() {
        override fun onBatteryLow(context: Context) {}
        override fun onBatteryOk(context: Context) {}
    }

    override fun new(context: Context): MainBatteryCallback = MainBatteryCallback()
}
