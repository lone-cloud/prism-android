package app.lonecloud.prism.activities

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lonecloud.prism.AppStore
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.Distributor
import app.lonecloud.prism.EventBus
import app.lonecloud.prism.PrismServerClient
import app.lonecloud.prism.services.FgService
import app.lonecloud.prism.services.MigrationManager
import app.lonecloud.prism.services.RestartWorker
import app.lonecloud.prism.services.SourceManager
import app.lonecloud.prism.utils.TAG
import kotlinx.coroutines.launch

class AppAction(private val action: Action) {
    sealed class Action {
        data object RestartService : Action()
        class ShowToasts(val enable: Boolean) : Action()
        class DeleteRegistration(val registrations: List<String>) : Action()
        data object FallbackIntroShown : Action()
        class FallbackDistribSelected(val distributor: String?) : Action()
        class MigrateToDistrib(val distributor: String) : Action()
        data object ReactivateUnifiedPush : Action()
        data object RegisterPrismServer : Action()
    }

    fun handle(context: Context) {
        when (action) {
            is Action.RestartService -> restartService(context)
            is Action.ShowToasts -> showToasts(context, action)
            is Action.DeleteRegistration -> deleteRegistration(context, action)
            is Action.FallbackIntroShown -> fallbackIntroShown(context)
            is Action.FallbackDistribSelected -> fallbackDistribSelected(context, action)
            is Action.MigrateToDistrib -> migrateToDistrib(context, action)
            is Action.ReactivateUnifiedPush -> reactivateUnifiedPush(context)
            is Action.RegisterPrismServer -> registerPrismServer(context)
        }
    }

    private fun restartService(context: Context) {
        Log.d(TAG, "Restarting the Listener")
        SourceManager.clearFails()
        FgService.stopService {
            RestartWorker.run(context, delay = 0)
        }
    }

    private fun showToasts(context: Context, action: Action.ShowToasts) {
        AppStore(context).showToasts = action.enable
    }

    private fun deleteRegistration(context: Context, action: Action.DeleteRegistration) {
        action.registrations.forEach { token ->
            val db = DatabaseFactory.getDb(context)
            val dbApp = db.listApps().find { it.connectorToken == token }

            if (dbApp?.description?.startsWith("target:") == true) {
                val appName = dbApp.title ?: dbApp.packageName
                PrismServerClient.deleteApp(context, appName)
            }

            Distributor.deleteApp(context, token)
        }
    }

    private fun fallbackIntroShown(context: Context) {
        MigrationManager().setFallbackIntroShown(context)
    }

    /**
     * Save fallback service
     *
     * If fallback is disabled and we have already send TEMP_UNAVAILABLE:
     * we send the endpoint again
     */
    private fun fallbackDistribSelected(context: Context, action: Action.FallbackDistribSelected) {
        MigrationManager()
            .selectFallbackService(
                context,
                action.distributor,
                SourceManager.shouldSendFallback
            )
    }

    private fun migrateToDistrib(context: Context, action: Action.MigrateToDistrib) {
        MigrationManager().migrate(context, action.distributor)
    }

    private fun reactivateUnifiedPush(context: Context) {
        MigrationManager().reactivate(context)
    }

    private fun registerPrismServer(context: Context) {
        app.lonecloud.prism.PrismServerClient.registerAllApps(context)
    }
}

fun ViewModel.publishAction(action: AppAction) {
    viewModelScope.launch {
        EventBus.publish(action)
    }
}
