package org.unifiedpush.distributor.sunup.services

import android.content.Context
import org.unifiedpush.distributor.Database
import org.unifiedpush.distributor.RegistrationCounter
import org.unifiedpush.distributor.ipc.sendUiAction
import org.unifiedpush.distributor.sunup.DatabaseFactory
import org.unifiedpush.distributor.sunup.utils.ForegroundNotification

object MainRegistrationCounter : RegistrationCounter() {

    override val workerCompanion = RestartWorker.Companion

    override fun hasManyFails(): Boolean = SourceManager.nFails > 1

    override fun onCountRefreshed(context: Context) {
        ForegroundNotification(context).update()
        sendUiAction(context, "REFRESH_REGISTRATIONS")
    }

    override fun getDb(context: Context): Database = DatabaseFactory.getDb(context)
}
