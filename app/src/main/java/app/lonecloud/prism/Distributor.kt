package app.lonecloud.prism

import android.content.Context
import app.lonecloud.prism.api.MessageSender
import app.lonecloud.prism.api.data.ClientMessage
import org.unifiedpush.distributor.Database
import org.unifiedpush.distributor.UnifiedPushDistributor

/**
 * These functions are used to send messages to other apps
 */
object Distributor : UnifiedPushDistributor() {
    override val receiverComponentName = "app.lonecloud.prism.receivers.RegisterBroadcastReceiver"

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
        MessageSender.send(
            context,
            ClientMessage.Unregister(
                channelID = channelId
            )
        )
    }
}
