package org.unifiedpush.distributor.sunup.utils

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean
import org.unifiedpush.distributor.AppNotification
import org.unifiedpush.distributor.sunup.R
import org.unifiedpush.distributor.sunup.activities.MainActivity
import org.unifiedpush.distributor.sunup.services.MainRegistrationCounter

const val NOTIFICATION_ID_FOREGROUND = 51115
private const val NOTIFICATION_ID_WARNING = 51215

class MainNotificationData(
    title: String,
    text: String,
    ticker: String,
    priority: Int,
    ongoing: Boolean
) : AppNotification.NotificationData(
    smallIcon = R.drawable.ic_logo,
    title = title,
    text = text,
    ticker = ticker,
    priority = priority,
    ongoing = ongoing,
    activity = MainActivity::class.java
)

private val Context.warningChannelData: AppNotification.ChannelData
    get() = AppNotification.ChannelData(
        "${this.getString(R.string.app_name)}.Warning",
        "Warning",
        NotificationManager.IMPORTANCE_HIGH,
        this.getString(R.string.warning_notif_description)
    )

class DisconnectedNotification(context: Context) : AppNotification(
    context,
    Notifications.disconnectedShown,
    NOTIFICATION_ID_WARNING,
    MainNotificationData(
        context.getString(R.string.app_name),
        context.getString(R.string.warning_notif_content),
        context.getString(R.string.warning_notif_ticker),
        Notification.PRIORITY_HIGH,
        true
    ),
    context.warningChannelData
)

class ForegroundNotification(context: Context) : AppNotification(
    context,
    Notifications.ignoreShown,
    NOTIFICATION_ID_FOREGROUND,
    MainNotificationData(
        context.getString(R.string.app_name),
        if (MainRegistrationCounter.oneOrMore(context)) {
            context.getString(R.string.foreground_notif_content_with_reg)
                .format(MainRegistrationCounter.getCount(context))
        } else {
            context.getString(R.string.foreground_notif_content_no_reg)
        },
        context.getString(R.string.foreground_notif_ticker),
        Notification.PRIORITY_LOW,
        true
    ),
    ChannelData(
        "${context.getString(R.string.app_name)}.Listener",
        "Foreground Service",
        NotificationManager.IMPORTANCE_LOW,
        context.getString(R.string.foreground_notif_description)
    )
)

private object Notifications {
    val disconnectedShown = AtomicBoolean(false)
    val ignoreShown = AtomicBoolean(true)
}
