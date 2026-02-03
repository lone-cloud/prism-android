package app.lonecloud.prism.utils

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import app.lonecloud.prism.R
import app.lonecloud.prism.activities.MainActivity
import app.lonecloud.prism.services.MainRegistrationCounter
import java.util.concurrent.atomic.AtomicBoolean
import org.unifiedpush.android.distributor.ui.R as LibR
import org.unifiedpush.distributor.AppNotification

const val NOTIFICATION_ID_FOREGROUND = 51115
private const val NOTIFICATION_ID_WARNING = 51215

class MainNotificationData(
    title: String,
    text: String,
    ticker: String,
    priority: Int,
    ongoing: Boolean
) : AppNotification.NotificationData(
    smallIcon = R.drawable.ic_notification,
    title = title,
    text = text,
    ticker = ticker,
    priority = priority,
    ongoing = ongoing,
    activity = MainActivity::class.java
)

private val Context.warningChannelData: AppNotification.ChannelData
    get() = AppNotification.ChannelData(
        "Warning",
        this.getString(LibR.string.warning),
        NotificationManager.IMPORTANCE_HIGH,
        this.resources.getString(LibR.string.warning_notif_description).format(this.getString(R.string.app_name))
    )

class DisconnectedNotification(context: Context) :
    AppNotification(
        context,
        Notifications.disconnectedShown,
        NOTIFICATION_ID_WARNING,
        MainNotificationData(
            context.getString(R.string.app_name),
            context.getString(LibR.string.warning_notif_content).format(
                context.getString(R.string.app_name)
            ),
            context.getString(LibR.string.warning),
            NotificationCompat.PRIORITY_HIGH,
            true
        ),
        context.warningChannelData
    )

class ForegroundNotification(context: Context) :
    AppNotification(
        context,
        Notifications.ignoreShown,
        NOTIFICATION_ID_FOREGROUND,
        MainNotificationData(
            context.getString(R.string.app_name),
            if (MainRegistrationCounter.oneOrMore(context)) {
                MainRegistrationCounter.getCount(context).let {
                    context.resources.getQuantityString(LibR.plurals.foreground_notif_content_with_reg, it, it)
                }
            } else {
                context.getString(LibR.string.foreground_notif_content_no_reg)
            },
            context.getString(LibR.string.foreground_service),
            NotificationCompat.PRIORITY_LOW,
            true
        ),
        ChannelData(
            "Foreground",
            context.getString(LibR.string.foreground_service),
            NotificationManager.IMPORTANCE_LOW,
            context.getString(LibR.string.foreground_notif_description)
        )
    )

private object Notifications {
    val disconnectedShown = AtomicBoolean(false)
    val ignoreShown = AtomicBoolean(true)
}
