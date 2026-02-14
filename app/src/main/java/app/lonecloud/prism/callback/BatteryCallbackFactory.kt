/*
 * Copyright (C) 2024 p1gp1g
 * Modified by lone-cloud under AGPL v3.0
 *
 * Original work: https://codeberg.org/Sunup/android
 * Licensed under Apache License 2.0
 */

package app.lonecloud.prism.callback

import android.content.Context
import org.unifiedpush.android.distributor.callback.BatteryCallback
import org.unifiedpush.android.distributor.callback.CallbackFactory

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
