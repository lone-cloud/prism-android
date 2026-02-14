package app.lonecloud.prism.services

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.graphics.drawable.toBitmap
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.Distributor
import app.lonecloud.prism.PrismPreferences
import org.unifiedpush.android.distributor.Database
import org.unifiedpush.android.distributor.MigrationManager
import org.unifiedpush.android.distributor.SourceManager
import org.unifiedpush.android.distributor.UnifiedPushDistributor
import org.unifiedpush.android.distributor.WorkerCompanion
import org.unifiedpush.android.distributor.data.App
import org.unifiedpush.android.distributor.data.Description
import org.unifiedpush.android.distributor.ipc.handler.IAccount
import org.unifiedpush.android.distributor.ipc.handler.IApi
import org.unifiedpush.android.distributor.ipc.handler.IRegistrations
import org.unifiedpush.android.distributor.service.ForegroundServiceFactory
import org.unifiedpush.android.distributor.service.InternalService
import org.unifiedpush.android.distributor.utils.getApplicationIcon

class PrismInternalService : InternalService() {
    override val sourceManager: SourceManager<*> = SourceManager
    override val restartWorker: WorkerCompanion = RestartWorker
    override val startService: ForegroundServiceFactory = FgService
    override val migrationManager: org.unifiedpush.android.distributor.MigrationManager = app.lonecloud.prism.services.MigrationManager()
    override val distributor: UnifiedPushDistributor = Distributor
    override val db: Database by lazy { DatabaseFactory.getDb(this) }

    private val appStore by lazy { PrismPreferences(this) }

    override var themeDynamicColors: Boolean
        get() = appStore.dynamicColors
        set(value) {
            appStore.dynamicColors = value
        }

    override var showToasts: Boolean
        get() = appStore.showToasts
        set(value) {
            appStore.showToasts = value
        }

    override fun getDebugInfo(): String = "Prism Distributor"

    override fun runAppMigration() {}

    override fun account(): IAccount = object : IAccount {
        override fun get(): String? = null
        override fun logout() {}
        override fun login(data: Bundle) {}
    }

    override fun api(): IApi = object : IApi {
        override fun newPushServer(url: String?) {}
        override fun getUrl(): String = appStore.apiUrl
    }

    override fun registrations() = object : IRegistrations {
        override fun delete(registrations: List<String>) {
            registrations.forEach { token ->
                distributor.deleteApp(this@PrismInternalService, token)
            }
        }

        override fun list(): List<App> = db
            .listApps().map {
                val pm = this@PrismInternalService.packageManager

                val isManualApp = it.description?.startsWith("target:") == true
                val targetPackage = if (isManualApp) {
                    it.description?.substringAfter("target:")?.substringBefore("|")?.takeIf { pkg -> pkg.isNotBlank() }
                } else {
                    null
                }

                val packageToResolve = (targetPackage ?: it.packageName) ?: ""
                val appName = try {
                    val appInfo = pm.getApplicationInfo(packageToResolve, PackageManager.GET_META_DATA)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    packageToResolve
                }

                val displayTitle = it.title ?: appName

                App(
                    connectorToken = it.connectorToken,
                    packageName = it.packageName,
                    endpoint = it.endpoint,
                    vapidKey = it.vapidKey,
                    title = displayTitle,
                    msgCount = it.msgCount,
                    description = if (it.packageName == this@PrismInternalService.packageName) {
                        Description.LocalChannel
                    } else {
                        Description.StringDescription(packageToResolve)
                    },
                    icon = getApplicationIcon(packageToResolve)?.toBitmap(),
                    isLocal = it.packageName == this@PrismInternalService.packageName
                )
            }

        override fun copyEndpoint(token: String?) {
        }

        override fun addLocal(title: String) {
        }
    }
}
