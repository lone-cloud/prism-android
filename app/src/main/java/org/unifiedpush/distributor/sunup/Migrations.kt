package org.unifiedpush.distributor.sunup

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import org.unifiedpush.distributor.MigrationFactory

class Migrations(context: Context) : MigrationFactory(context, AppStore.PREF_NAME) {

    override val migrations = listOf(
        Migration000201
    )

    /**
     * Migration from 0.x.x to 0.2.1
     */
    object Migration000201 : Migration {
        override val version = 201
        override fun run(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                removeOldOsNotificationChannel(context)
            }
        }

        /**
         * Migration to remove old notifications channel, they will be recreated
         * once needed. We used app_name as a prefix, but this can be buggy with
         * debug env.
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun removeOldOsNotificationChannel(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val oldPrefix = context.getString(R.string.app_name)
            notificationManager.notificationChannels.forEach {
                if (it.id.startsWith(oldPrefix)) {
                    notificationManager.deleteNotificationChannel(it.id)
                }
            }
        }
    }
}