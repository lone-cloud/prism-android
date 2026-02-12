package app.lonecloud.prism.services

import android.content.Context
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.utils.ForegroundNotification
import org.unifiedpush.android.distributor.Database
import org.unifiedpush.android.distributor.RegistrationCounter
import org.unifiedpush.android.distributor.ipc.sendUiAction

object MainRegistrationCounter : RegistrationCounter() {

    override val workerCompanion = RestartWorker.Companion

    override fun hasManyFails(): Boolean = SourceManager.nFails > 1

    override fun onCountRefreshed(context: Context) {
        ForegroundNotification(context).update()
        sendUiAction(context, "RefreshRegistrations")
    }

    override fun getDb(context: Context): Database = DatabaseFactory.getDb(context)
}
