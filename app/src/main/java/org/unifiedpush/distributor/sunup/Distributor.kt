package org.unifiedpush.distributor.sunup

import android.content.Context
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.unifiedpush.distributor.ChannelCreationStatus
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

    @OptIn(ExperimentalUuidApi::class)
    override fun registerChannelIdToServer(
        context: Context,
        packageName: String,
        channelId: String?,
        title: String?,
        vapid: String?,
        callback: (ChannelCreationStatus) -> Unit
    ) {
        val uuid = channelId ?: Uuid.random().toString()
        MessageSender.send(
            context,
            ClientMessage.Register(
                channelID = uuid,
                key = vapid
            )
        )
        // REGISTER will be send later, when we received the new endpoint from autopush
        callback(ChannelCreationStatus.Ok(channelId = uuid, sendEndpoint = false))
    }

    override fun unregisterChannelIdToServer(context: Context, appToken: String, callback: (Boolean) -> Unit) {
        MessageSender.send(
            context,
            ClientMessage.Unregister(
                channelID = appToken
            )
        )
        callback(true)
    }
}
