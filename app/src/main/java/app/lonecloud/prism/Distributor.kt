package app.lonecloud.prism

import android.content.Context
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
        val db = getDb(context)
        val app = db.listApps().find { it.connectorToken == channelId }
        if (app?.description?.startsWith("target:") == true) {
            val appName = app.title ?: app.packageName
            PrismServerClient.deleteApp(context, appName)
        }

        MessageSender.send(
            context,
            ClientMessage.Unregister(
                channelID = channelId
            )
        )
    }
}
