package org.unifiedpush.distributor.sunup

import android.content.Context
import org.unifiedpush.distributor.Database
import org.unifiedpush.distributor.UnifiedPushDistributor
import org.unifiedpush.distributor.sunup.api.MessageSender
import org.unifiedpush.distributor.sunup.api.data.ClientMessage

/**
 * These functions are used to send messages to other apps
 */
object Distributor : UnifiedPushDistributor() {
    override fun getDb(context: Context): Database {
        return DatabaseFactory.getDb(context)
    }

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
        MessageSender.send(
            context,
            ClientMessage.Unregister(
                channelID = channelId
            )
        )
    }
}
