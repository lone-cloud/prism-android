/*
 * Copyright (C) 2024 p1gp1g
 * Modified by lone-cloud under AGPL v3.0
 *
 * Original work: https://codeberg.org/Sunup/android
 * Licensed under Apache License 2.0
 */

package app.lonecloud.prism.utils

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import app.lonecloud.prism.R
import app.lonecloud.prism.services.MainRegistrationCounter
import java.util.concurrent.atomic.AtomicBoolean
import org.unifiedpush.android.distributor.AppNotification

const val NOTIFICATION_ID_FOREGROUND = 51115
private const val NOTIFICATION_ID_WARNING = 51215
private const val WARNING_CHANNEL_ID = "WarningSilent"
private const val FOREGROUND_CHANNEL_ID = "ForegroundSilent"

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
    ongoing = ongoing
)

private val Context.warningChannelData: AppNotification.ChannelData
    get() = AppNotification.ChannelData(
        WARNING_CHANNEL_ID,
        this.getString(R.string.warning),
        NotificationManager.IMPORTANCE_LOW,
        this.resources.getString(R.string.warning_notif_description).format(this.getString(R.string.app_name))
    )

class DisconnectedNotification(context: Context) :
    AppNotification(
        context,
        Notifications.disconnectedShown,
        NOTIFICATION_ID_WARNING,
        MainNotificationData(
            context.getString(R.string.app_name),
            context.getString(R.string.warning_notif_content).format(
                context.getString(R.string.app_name)
            ),
            context.getString(R.string.warning),
            NotificationCompat.PRIORITY_LOW,
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
                    context.resources.getQuantityString(R.plurals.foreground_notif_content_with_reg, it, it)
                }
            } else {
                context.getString(R.string.foreground_notif_content_no_reg)
            },
            context.getString(R.string.foreground_service),
            NotificationCompat.PRIORITY_LOW,
            true
        ),
        ChannelData(
            FOREGROUND_CHANNEL_ID,
            context.getString(R.string.foreground_service),
            NotificationManager.IMPORTANCE_LOW,
            context.getString(R.string.foreground_notif_description)
        )
    )

private object Notifications {
    val disconnectedShown = AtomicBoolean(true)
    val ignoreShown = AtomicBoolean(true)
}
