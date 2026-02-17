package app.lonecloud.prism.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import app.lonecloud.prism.R
import app.lonecloud.prism.api.data.NotificationAction
import app.lonecloud.prism.api.data.NotificationPayload
import app.lonecloud.prism.receivers.NotificationActionReceiver
import app.lonecloud.prism.services.MainRegistrationCounter
import org.unifiedpush.android.distributor.Database

private const val NOTIFICATION_BASE_ID = 52000

object ManualAppNotifications {

    private val notificationIds = mutableMapOf<String, Int>()
    private var nextNotificationId = NOTIFICATION_BASE_ID

    fun showNotification(
        context: Context,
        channelID: String,
        app: Database.App,
        payload: NotificationPayload
    ) {
        if (payload.title.isBlank() && payload.message.isBlank()) {
            dismissNotification(context, payload.tag)
            return
        }

        val channelId = "manual_app_${app.connectorToken}"
        val appTitle = app.title ?: "Unknown App"
        createNotificationChannel(context, channelId, appTitle)

        val notificationId = getNotificationId(payload.tag)
        val packageName = extractTargetPackage(app.description)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(payload.title)
            .setContentText(payload.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup(app.connectorToken)

        val contentIntent = createContentIntent(context, packageName, notificationId)
        if (contentIntent != null) {
            notificationBuilder.setContentIntent(contentIntent)
        }

        payload.actions.take(3).forEach { action ->
            val actionIntent = createActionIntent(
                context,
                channelID,
                action,
                payload.tag,
                notificationId
            )
            notificationBuilder.addAction(
                0,
                action.label,
                actionIntent
            )
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(payload.tag, notificationId, notificationBuilder.build())

        updateMessageCount(context)

        Log.d(TAG, "Displayed notification for manual app '${app.title}': ${payload.title} (tag: ${payload.tag})")
    }

    fun dismissNotification(context: Context, tag: String) {
        val notificationId = notificationIds[tag]
        if (notificationId != null) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(tag, notificationId)
            notificationIds.remove(tag)
            Log.d(TAG, "Dismissed notification with tag: $tag")
        } else {
            Log.w(TAG, "Cannot dismiss notification - tag not found: $tag")
        }
    }

    private fun createNotificationChannel(
        context: Context,
        channelId: String,
        appName: String
    ) {
        val channel = NotificationChannel(
            channelId,
            appName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun getNotificationId(tag: String): Int = notificationIds.getOrPut(tag) {
        nextNotificationId++
    }

    private fun extractTargetPackage(description: String?): String? {
        if (description == null || !description.startsWith("target:")) return null
        return description.substringAfter("target:").substringBefore("|").takeIf { it.isNotBlank() }
    }

    private fun createContentIntent(
        context: Context,
        packageName: String?,
        notificationId: Int
    ): PendingIntent? {
        if (packageName == null) return null

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            Log.w(TAG, "Could not create launch intent for package: $packageName")
            null
        }
    }

    private fun createActionIntent(
        context: Context,
        channelID: String,
        action: NotificationAction,
        notificationTag: String,
        notificationId: Int
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra("channelID", channelID)
            putExtra("actionID", action.id)
            putExtra("actionLabel", action.label)
            putExtra("actionEndpoint", action.endpoint)
            putExtra("actionMethod", action.method)
            putExtra("notificationTag", notificationTag)
            putExtra("notificationId", notificationId)

            action.data.forEach { (key, value) ->
                putExtra("data_$key", value.toString())
            }
        }

        val requestCode = (channelID + action.id).hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateMessageCount(context: Context) {
        MainRegistrationCounter.onCountRefreshed(context)
    }
}
