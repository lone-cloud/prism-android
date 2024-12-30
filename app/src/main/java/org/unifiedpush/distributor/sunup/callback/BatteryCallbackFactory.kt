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
    }

    override fun new(context: Context): MainBatteryCallback {
        if (BuildConfig.URGENCY) {
            MessageSender.send(context, ClientMessage.MinUrgency(ClientMessage.Urgency.VeryLow))
        }
        return MainBatteryCallback()
    }

    /**
     * Default to false
     */
    val lowBattery: Boolean = instance?.isLowBattery() ?: false
}
