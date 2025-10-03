package org.unifiedpush.distributor.sunup.activities

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.unifiedpush.distributor.sunup.AppStore
import org.unifiedpush.distributor.sunup.Distributor
import org.unifiedpush.distributor.sunup.EventBus
import org.unifiedpush.distributor.sunup.api.ApiUrlCandidate
import org.unifiedpush.distributor.sunup.services.FgService
import org.unifiedpush.distributor.sunup.services.MigrationManager
import org.unifiedpush.distributor.sunup.services.RestartWorker
import org.unifiedpush.distributor.sunup.services.SourceManager
import org.unifiedpush.distributor.sunup.utils.TAG

class AppAction(private val action: Action) {
    sealed class Action {
        data object RestartService : Action()
        class NewPushServer(val url: String) : Action()
        class ShowToasts(val enable: Boolean) : Action()
        class DeleteRegistration(val registrations: List<String>) : Action()
        data object FallbackIntroShown : Action()
        class FallbackDistribSelected(val distributor: String?) : Action()
        class MigrateToDistrib(val distributor: String) : Action()
        data object ReactivateUnifiedPush : Action()
    }

    fun handle(context: Context) {
        when (action) {
            is Action.RestartService -> restartService(context)
            is Action.NewPushServer -> newPushServer(context, action)
            is Action.ShowToasts -> showToasts(context, action)
            is Action.DeleteRegistration -> deleteRegistration(context, action)
            is Action.FallbackIntroShown -> fallbackIntroShown(context)
            is Action.FallbackDistribSelected -> fallbackDistribSelected(context, action)
            is Action.MigrateToDistrib -> migrateToDistrib(context, action)
            is Action.ReactivateUnifiedPush -> reactivateUnifiedPush(context)
        }
    }

    private fun restartService(context: Context) {
        Log.d(TAG, "Restarting the Listener")
        SourceManager.clearFails()
        FgService.stopService {
            RestartWorker.run(context, delay = 0)
        }
    }

    private fun newPushServer(context: Context, action: Action.NewPushServer) {
        ApiUrlCandidate.test(context, action.url)
    }

    private fun showToasts(context: Context, action: Action.ShowToasts) {
        AppStore(context).showToasts = action.enable
    }

    private fun deleteRegistration(context: Context, action: Action.DeleteRegistration) {
        action.registrations.forEach {
            Distributor.deleteApp(context, it)
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
}

fun ViewModel.publishAction(action: AppAction) {
    viewModelScope.launch {
        EventBus.publish(action)
    }
}
