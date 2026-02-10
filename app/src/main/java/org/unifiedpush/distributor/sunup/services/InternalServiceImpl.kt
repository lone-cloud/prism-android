package org.unifiedpush.distributor.sunup.services

import android.os.Bundle
import java.text.SimpleDateFormat
import org.unifiedpush.distributor.Database
import org.unifiedpush.distributor.MigrationManager
import org.unifiedpush.distributor.SourceManager
import org.unifiedpush.distributor.UnifiedPushDistributor
import org.unifiedpush.distributor.WorkerCompanion
import org.unifiedpush.distributor.ipc.ACTION_REFRESH_API_URL
import org.unifiedpush.distributor.ipc.handler.IAccount
import org.unifiedpush.distributor.ipc.handler.IApi
import org.unifiedpush.distributor.ipc.sendUiAction
import org.unifiedpush.distributor.service.ForegroundServiceFactory
import org.unifiedpush.distributor.service.InternalService
import org.unifiedpush.distributor.sunup.AppStore
import org.unifiedpush.distributor.sunup.BuildConfig
import org.unifiedpush.distributor.sunup.DatabaseFactory
import org.unifiedpush.distributor.sunup.Distributor
import org.unifiedpush.distributor.sunup.api.ApiUrlCandidate
import org.unifiedpush.distributor.sunup.api.ServerConnection

class InternalServiceImpl : InternalService() {
    override val sourceManager: SourceManager<*>
        get() = SourceManager
    override val restartWorker: WorkerCompanion
        get() = RestartWorker.Companion
    override val startService: ForegroundServiceFactory
        get() = FgService.Companion
    override val migrationManager: MigrationManager
        get() = MigrationManagerImpl()
    override val distributor: UnifiedPushDistributor
        get() = Distributor
    override val db: Database
        get() = DatabaseFactory.getDb(this)

    override fun getDebugInfo(): String {
        val date = ServerConnection.lastEventDate?.let {
            SimpleDateFormat.getDateTimeInstance().format(it.time)
        } ?: "None"
        return "ServiceStarted: ${FgService.isServiceStarted()}\n" +
            "Last Event: $date\n" +
            org.unifiedpush.distributor.sunup.services.SourceManager.getDebugInfo()
    }

    /**
     * Not used by Sunup
     */
    override fun account() = object : IAccount {
        override fun get(): String? = null
        override fun logout() {}
        override fun login(data: Bundle) {}
    }

    override fun api() = object : IApi {
        override fun newPushServer(url: String?) {
            url?.let {
                ApiUrlCandidate.test(context, url)
            } ?: run {
                AppStore(context).apiUrl = BuildConfig.DEFAULT_API_URL
                restartWorker().restart()
                sendUiAction(context, ACTION_REFRESH_API_URL)
            }
        }

        override fun getUrl() = AppStore(context).apiUrl
    }

    override var themeDynamicColors: Boolean
        get() = AppStore(context).dynamicColors
        set(value) {
            AppStore(context).dynamicColors = value
        }

    override var showToasts: Boolean
        get() = AppStore(context).showToasts
        set(value) {
            AppStore(context).showToasts = value
        }
}
