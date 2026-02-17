/*
 * Copyright (C) 2024 p1gp1g
 * Modified by lone-cloud under AGPL v3.0
 *
 * Original work: https://codeberg.org/Sunup/android
 * Licensed under Apache License 2.0
 */

package app.lonecloud.prism

import android.content.Context
import android.util.Log
import app.lonecloud.prism.api.MessageSender
import app.lonecloud.prism.api.data.ClientMessage
import org.unifiedpush.android.distributor.Database
import org.unifiedpush.android.distributor.UnifiedPushDistributor

object Distributor : UnifiedPushDistributor() {
    override val receiverComponent = app.lonecloud.prism.receivers.RegisterBroadcastReceiver::class.java

    override fun getDb(context: Context): Database = DatabaseFactory.getDb(context)

    override fun backendRegisterNewChannelId(
        context: Context,
        packageName: String,
        channelId: String,
        title: String?,
        vapid: String?,
        description: String?
    ) {
        val uuid = channelId
        MessageSender.send(
            context,
            ClientMessage.Register(
                channelID = uuid,
                key = vapid
            )
        )
    }

    override fun backendUpdateChannelId(
        context: Context,
        packageName: String,
        channelId: String,
        title: String?,
        vapid: String?,
        description: String?
    ) = backendRegisterNewChannelId(context, packageName, channelId, title, vapid, description)

    override fun backendUnregisterChannelId(context: Context, channelId: String) {
        Log.d("Distributor", "backendUnregisterChannelId called with channelId: $channelId")

        val db = getDb(context)

        val channelVapidPair = db.listChannelIdVapid().find { it.first == channelId }
        Log.d("Distributor", "Found vapidKey for channelId: ${channelVapidPair?.second}")

        if (channelVapidPair != null) {
            val app = db.listApps().find { it.vapidKey == channelVapidPair.second }
            Log.d(
                "Distributor",
                "Found app: ${app?.title}, isManual: ${app?.description?.startsWith("target:")}, connectorToken: ${app?.connectorToken}"
            )

            if (app?.description?.startsWith("target:") == true) {
                Log.d("Distributor", "Calling PrismServerClient.deleteApp with connectorToken: ${app.connectorToken}")
                PrismServerClient.deleteApp(context, app.connectorToken)
            }
        } else {
            Log.w("Distributor", "No vapidKey found for channelId: $channelId")
        }

        MessageSender.send(
            context,
            ClientMessage.Unregister(
                channelID = channelId
            )
        )
    }
}
