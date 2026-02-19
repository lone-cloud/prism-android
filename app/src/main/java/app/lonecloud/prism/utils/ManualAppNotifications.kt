package app.lonecloud.prism.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.R
import app.lonecloud.prism.api.data.NotificationAction
import app.lonecloud.prism.api.data.NotificationPayload
import app.lonecloud.prism.receivers.NotificationActionReceiver
import app.lonecloud.prism.services.MainRegistrationCounter
import org.unifiedpush.android.distributor.Database

private const val NOTIFICATION_BASE_ID = 52000
private const val MANUAL_CHANNEL_PREFIX = "manual_app_"

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
            if (payload.tag.isNotBlank()) {
                dismissNotification(context, payload.tag)
            } else {
                Log.w(TAG, "Ignoring empty payload notification without tag for ${app.title}")
            }
            return
        }

        val channelId = channelIdForToken(app.connectorToken)
        val appTitle = app.title ?: "Unknown App"
        createNotificationChannel(context, channelId, appTitle)

        val hasTitle = payload.title.isNotBlank()
        val hasMessage = payload.message.isNotBlank()
        val sender = if (hasTitle && hasMessage) payload.title else null
        val bodyText = when {
            hasMessage -> payload.message
            hasTitle -> payload.title
            else -> ""
        }

        val notificationId = getNotificationId(payload.tag)
        val packageName = resolveTargetPackage(app)

        val contentText = sender?.let { "$it: $bodyText" } ?: bodyText
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(bodyText)
            .also { style ->
                sender?.let { style.setSummaryText(it) }
            }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(appTitle)
            .setContentText(contentText)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup(app.connectorToken)

        resolveAppIconBitmap(context, packageName)?.let { appIcon ->
            notificationBuilder.setLargeIcon(appIcon)
        }

        val contentIntent = createContentIntent(context, packageName, notificationId)
        if (contentIntent != null) {
            notificationBuilder.setContentIntent(contentIntent)
        }

        payload.actions
            .filter { it.label.isNotBlank() && it.endpoint.isNotBlank() }
            .take(3)
            .forEach { action ->
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
        notificationManager.notify(
            payload.tag,
            notificationId,
            notificationBuilder.build()
        )

        incrementMessageCount(context, app)
        refreshMessageCount(context)

        val previewSender = sender ?: ""
        val logMessage =
            "Displayed notification for manual app '${app.title}' " +
                "sender='$previewSender' body='${bodyText.take(120)}' (tag: ${payload.tag})"
        Log.d(TAG, logMessage)
    }

    private fun resolveAppIconBitmap(context: Context, packageName: String?): android.graphics.Bitmap? {
        if (packageName.isNullOrBlank()) return null

        return try {
            context.packageManager.getApplicationIcon(packageName).toBitmap()
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Could not resolve app icon for package: $packageName",
                e
            )
            null
        }
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

    fun deleteChannelForToken(context: Context, connectorToken: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.deleteNotificationChannel(channelIdForToken(connectorToken))
    }

    fun pruneOrphanedChannels(context: Context) {
        val db = DatabaseFactory.getDb(context)
        val validManualChannelIds = db.listApps()
            .asSequence()
            .map { it.connectorToken }
            .filter { it.startsWith(MANUAL_CHANNEL_PREFIX) }
            .map { channelIdForToken(it) }
            .toSet()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notificationChannels
            .asSequence()
            .map { it.id }
            .filter { it.startsWith(MANUAL_CHANNEL_PREFIX) }
            .filterNot { it in validManualChannelIds }
            .forEach { notificationManager.deleteNotificationChannel(it) }
    }

    private fun channelIdForToken(connectorToken: String): String = "$MANUAL_CHANNEL_PREFIX$connectorToken"

    private fun getNotificationId(tag: String): Int = notificationIds.getOrPut(tag) {
        nextNotificationId++
    }

    private fun resolveTargetPackage(app: Database.App): String? {
        val appPackage = app.packageName.takeIf { it.isNotBlank() }
        if (appPackage != null && appPackage != "app.lonecloud.prism" && appPackage != "app.lonecloud.prism.debug") {
            return appPackage
        }

        return app.description
            ?.split("|")
            ?.firstOrNull { it.startsWith("target:") }
            ?.substringAfter("target:")
            ?.takeIf { it.isNotBlank() }
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

    private fun incrementMessageCount(context: Context, app: Database.App) {
        val db = DatabaseFactory.getDb(context)
        val currentCount = db.getAppFromCoToken(app.connectorToken)?.msgCount ?: app.msgCount
        db.setMsgCount(app.connectorToken, currentCount + 1)
    }

    private fun refreshMessageCount(context: Context) {
        MainRegistrationCounter.onCountRefreshed(context)
    }
}
