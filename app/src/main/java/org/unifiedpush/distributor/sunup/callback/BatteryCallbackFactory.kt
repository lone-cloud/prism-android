package org.unifiedpush.distributor.sunup.callback

import android.content.Context
import org.unifiedpush.distributor.callback.BatteryCallback
import org.unifiedpush.distributor.callback.CallbackFactory
import org.unifiedpush.distributor.sunup.BuildConfig
import org.unifiedpush.distributor.sunup.api.MessageSender
import org.unifiedpush.distributor.sunup.api.data.ClientMessage

object BatteryCallbackFactory : CallbackFactory<BatteryCallbackFactory.MainBatteryCallback>() {

    class MainBatteryCallback : BatteryCallback() {

        override fun onBatteryLow(context: Context) {
            if (BuildConfig.URGENCY) {
                MessageSender.send(context, ClientMessage.MinUrgency(ClientMessage.Urgency.Normal))
            }
        }

        override fun onBatteryOk(context: Context) {
            if (BuildConfig.URGENCY) {
                MessageSender.send(context, ClientMessage.MinUrgency(ClientMessage.Urgency.VeryLow))
            }
        }

        /**
         * Once the battery callback is registered,
         * we send min urgency depending on the battery level
         */
        override fun register(context: Context) {
            super.register(context)
            if (lowBattery) {
                onBatteryLow(context)
            } else {
                onBatteryOk(context)
            }
        }
    }

    override fun new(context: Context): MainBatteryCallback = MainBatteryCallback()
}
