package app.lonecloud.prism.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
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
import app.lonecloud.prism.receivers.NotificationDismissReceiver
import app.lonecloud.prism.services.MainRegistrationCounter
import org.unifiedpush.android.distributor.Database

private const val NOTIFICATION_BASE_ID = 52000
private const val MANUAL_CHANNEL_PREFIX = "manual_app_"

object ManualAppNotifications {

    private val notificationIds = mutableMapOf<String, Int>()
    private val notificationConnectorTokens = mutableMapOf<String, String>()
    private val summaryNotificationIds = mutableMapOf<String, Int>()
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

        val notificationId = getNotificationId(payload.tag)
        notificationConnectorTokens[payload.tag] = app.connectorToken
        val packageName = resolveTargetPackage(app)

        val contentTitle = if (hasTitle) payload.title else appTitle
        val contentText = if (hasMessage) payload.message else ""

        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(contentText)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup(app.connectorToken)
            .apply {
                if (hasTitle) setSubText(appTitle)
            }

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
                    app.connectorToken,
                    payload.tag
                )
                notificationBuilder.addAction(
                    0,
                    action.label,
                    actionIntent
                )
            }

        notificationBuilder.setDeleteIntent(
            createDismissIntent(
                context = context,
                connectorToken = app.connectorToken,
                notificationTag = payload.tag
            )
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            payload.tag,
            notificationId,
            notificationBuilder.build()
        )

        postGroupSummary(context, app.connectorToken, channelId, packageName)

        incrementMessageCount(context, app)
        refreshMessageCount(context)

        val logMessage =
            "Displayed notification for manual app '${app.title}' " +
                "title='${payload.title.take(80)}' body='${contentText.take(120)}' (tag: ${payload.tag})"
        Log.d(TAG, logMessage)
    }

    private fun resolveAppIconBitmap(context: Context, packageName: String?): android.graphics.Bitmap? {
        if (packageName.isNullOrBlank()) return null

        return try {
            context.packageManager.getApplicationIcon(packageName).toBitmap()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(
                TAG,
                "Could not resolve app icon for package: $packageName",
                e
            )
            null
        } catch (e: IllegalArgumentException) {
            Log.w(
                TAG,
                "Could not resolve app icon for package: $packageName",
                e
            )
            null
        }
    }

    fun dismissNotification(
        context: Context,
        tag: String,
        connectorTokenHint: String? = null
    ) {
        val notificationId = notificationIds[tag]
        val connectorToken = notificationConnectorTokens[tag] ?: connectorTokenHint
        if (notificationId != null) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(tag, notificationId)
            notificationIds.remove(tag)
            notificationConnectorTokens.remove(tag)
            connectorToken?.let { dismissSummaryIfGroupEmpty(context, it) }
            Log.d(TAG, "Dismissed notification with tag: $tag")
        } else {
            connectorToken?.let { dismissSummaryIfGroupEmpty(context, it) }
            Log.w(TAG, "Cannot dismiss notification - tag not found: $tag")
        }
    }

    private fun dismissSummaryIfGroupEmpty(context: Context, connectorToken: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val hasRemainingChildren = notificationManager.activeNotifications.any { statusBarNotification ->
            statusBarNotification.notification.group == connectorToken &&
                (statusBarNotification.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
        }
        if (hasRemainingChildren) return

        summaryNotificationIds.remove(connectorToken)?.let { summaryId ->
            notificationManager.cancel(summaryId)
        }

        notificationManager.activeNotifications
            .filter { statusBarNotification ->
                statusBarNotification.notification.group == connectorToken &&
                    (statusBarNotification.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            }
            .forEach { statusBarNotification ->
                notificationManager.cancel(statusBarNotification.tag, statusBarNotification.id)
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

    private fun getSummaryNotificationId(connectorToken: String): Int = summaryNotificationIds.getOrPut(connectorToken) {
        nextNotificationId++
    }

    private fun postGroupSummary(
        context: Context,
        connectorToken: String,
        channelId: String,
        packageName: String?
    ) {
        val summaryId = getSummaryNotificationId(connectorToken)
        val summaryBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setGroup(connectorToken)
            .setGroupSummary(true)
            .setAutoCancel(true)

        val contentIntent = packageName?.let { createContentIntent(context, it, summaryId) }
        if (contentIntent != null) {
            summaryBuilder.setContentIntent(contentIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(summaryId, summaryBuilder.build())
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
        connectorToken: String,
        notificationTag: String
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra("channelID", channelID)
            putExtra("actionID", action.id)
            putExtra("actionLabel", action.label)
            putExtra("actionEndpoint", action.endpoint)
            putExtra("actionMethod", action.method)
            putExtra("connectorToken", connectorToken)
            putExtra("notificationTag", notificationTag)

            action.data.forEach { (key, value) ->
                putExtra("data_$key", value.toString())
            }
        }

        val requestCode = (channelID + action.id + notificationTag).hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createDismissIntent(
        context: Context,
        connectorToken: String,
        notificationTag: String
    ): PendingIntent {
        val intent = Intent(context, NotificationDismissReceiver::class.java).apply {
            putExtra("connectorToken", connectorToken)
            putExtra("notificationTag", notificationTag)
        }

        return PendingIntent.getBroadcast(
            context,
            (connectorToken + notificationTag + "dismiss").hashCode(),
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
