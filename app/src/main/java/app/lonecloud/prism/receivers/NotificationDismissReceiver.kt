package app.lonecloud.prism.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.lonecloud.prism.utils.ManualAppNotifications

class NotificationDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationTag = intent.getStringExtra("notificationTag") ?: return
        val connectorToken = intent.getStringExtra("connectorToken")
        ManualAppNotifications.dismissNotification(context, notificationTag, connectorToken)
    }
}
