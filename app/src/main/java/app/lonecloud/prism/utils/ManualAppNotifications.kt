package app.lonecloud.prism.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import app.lonecloud.prism.DatabaseFactory
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
            if (payload.tag.isNotBlank()) {
                dismissNotification(context, payload.tag)
            } else {
                Log.w(TAG, "Ignoring empty payload notification without tag for ${app.title}")
            }
            return
        }

        val channelId = "manual_app_${app.connectorToken}"
        val appTitle = app.title ?: "Unknown App"
        createNotificationChannel(context, channelId, appTitle)
        val displayTitle = payload.title.ifBlank { appTitle }

        val notificationId = getNotificationId(payload.tag)
        val packageName = resolveTargetPackage(app)
        val appPerson = buildNotificationPerson(context, appTitle, packageName)
        val messageStyle = NotificationCompat.MessagingStyle(appPerson)
            .setConversationTitle(displayTitle)
            .addMessage(payload.message, System.currentTimeMillis(), appPerson)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(displayTitle)
            .setContentText(payload.message)
            .setStyle(messageStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup(app.connectorToken)

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
        notificationManager.notify(payload.tag, notificationId, notificationBuilder.build())

        incrementMessageCount(context, app)
        refreshMessageCount(context)

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

    private fun buildNotificationPerson(
        context: Context,
        appTitle: String,
        packageName: String?
    ): Person {
        val personBuilder = Person.Builder()
            .setName(appTitle)

        resolveAppIconBitmap(context, packageName)?.let { iconBitmap ->
            personBuilder.setIcon(IconCompat.createWithBitmap(iconBitmap))
        }

        return personBuilder.build()
    }

    private fun resolveAppIconBitmap(context: Context, packageName: String?): Bitmap? {
        if (packageName == null) return null

        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap()
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve app icon for package: $packageName", e)
            null
        }
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable && bitmap != null) {
            return bitmap
        }

        val width = intrinsicWidth.takeIf { it > 0 } ?: 128
        val height = intrinsicHeight.takeIf { it > 0 } ?: 128
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}
