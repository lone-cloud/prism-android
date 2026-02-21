/*
 * Copyright (C) 2024 p1gp1g
 * Modified by lone-cloud under AGPL v3.0
 *
 * Original work: https://codeberg.org/Sunup/android
 * Licensed under Apache License 2.0
 */

package app.lonecloud.prism

import android.content.Context
import app.lonecloud.prism.api.MessageSender
import app.lonecloud.prism.api.data.ClientMessage
import app.lonecloud.prism.utils.DescriptionParser
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
        MessageSender.send(
            context,
            ClientMessage.Register(
                channelID = channelId,
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
        val db = getDb(context)

        val channelVapidPair = db.listChannelIdVapid().find { it.first == channelId }

        if (channelVapidPair != null) {
            val app = db.listApps().find { it.vapidKey == channelVapidPair.second }
            if (app != null && DescriptionParser.isManualApp(app.description)) {
                PrismServerClient.deleteApp(context, app.connectorToken)
            }
        }

        MessageSender.send(
            context,
            ClientMessage.Unregister(
                channelID = channelId
            )
        )
    }
}
