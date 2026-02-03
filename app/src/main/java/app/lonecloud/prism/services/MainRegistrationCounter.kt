package app.lonecloud.prism.services

import android.content.Context
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.activities.UiAction
import app.lonecloud.prism.utils.ForegroundNotification
import org.unifiedpush.distributor.Database
import org.unifiedpush.distributor.RegistrationCounter

object MainRegistrationCounter : RegistrationCounter() {

    override val workerCompanion = RestartWorker.Companion

    override fun hasManyFails(): Boolean = SourceManager.nFails > 1

    override fun onCountRefreshed(context: Context) {
        ForegroundNotification(context).update()
        UiAction.publish(UiAction.Action.RefreshRegistrations)
    }

    override fun getDb(context: Context): Database = DatabaseFactory.getDb(context)
}
