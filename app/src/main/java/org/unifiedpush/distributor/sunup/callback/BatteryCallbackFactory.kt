package org.unifiedpush.distributor.sunup.callback

import android.content.Context
import org.unifiedpush.distributor.callback.BatteryCallback
import org.unifiedpush.distributor.callback.CallbackFactory

object BatteryCallbackFactory : CallbackFactory<BatteryCallbackFactory.MainBatteryCallback>() {

    class MainBatteryCallback : BatteryCallback() {

        override fun onBatteryLow(context: Context) {
            // TODO: Subscribe to normal and high urgency messages only
        }

        override fun onBatteryOk(context: Context) {
            // TODO: Subscribe back to all messages
        }
    }

    override fun new(context: Context): MainBatteryCallback {
        return MainBatteryCallback()
    }

    /**
     * Default to false
     */
    val lowBattery: Boolean = instance?.isLowBattery() ?: false
}
