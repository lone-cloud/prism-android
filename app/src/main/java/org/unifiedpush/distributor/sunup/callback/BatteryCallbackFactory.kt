package org.unifiedpush.distributor.sunup.callback

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean
import org.unifiedpush.distributor.callback.BatteryCallback
import org.unifiedpush.distributor.callback.CallbackFactory
import org.unifiedpush.distributor.sunup.BuildConfig
import org.unifiedpush.distributor.sunup.api.MessageSender
import org.unifiedpush.distributor.sunup.api.data.ClientMessage

object BatteryCallbackFactory : CallbackFactory<BatteryCallbackFactory.MainBatteryCallback>() {

    class MainBatteryCallback : BatteryCallback() {
        override val lowBattery = BatteryCallbackFactory.lowBattery

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
            if (isLowBattery()) {
                onBatteryLow(context)
            } else {
                onBatteryOk(context)
            }
        }
    }

    override fun new(context: Context): MainBatteryCallback = MainBatteryCallback()

    /**
     * Default to false
     */
    private val lowBattery = AtomicBoolean(false)

    fun isLowBattery(): Boolean = lowBattery.get()
}
