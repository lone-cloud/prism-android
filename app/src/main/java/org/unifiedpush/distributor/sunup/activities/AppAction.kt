package org.unifiedpush.distributor.sunup.activities

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.unifiedpush.distributor.sunup.Distributor
import org.unifiedpush.distributor.sunup.EventBus
import org.unifiedpush.distributor.sunup.api.ApiUrlCandidate
import org.unifiedpush.distributor.sunup.services.FgService
import org.unifiedpush.distributor.sunup.services.RestartWorker
import org.unifiedpush.distributor.sunup.services.SourceManager
import org.unifiedpush.distributor.sunup.utils.TAG

class AppAction(private val action: Action) {
    sealed class Action {
        data object RestartService : Action()
        class NewPushServer(val url: String) : Action()
        class DeleteRegistration(val registrations: List<String>) : Action()
    }

    fun handle(context: Context) {
        when (action) {
            is Action.RestartService -> restartService(context)
            is Action.NewPushServer -> newPushServer(context, action)
            is Action.DeleteRegistration -> deleteRegistration(context, action)
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
        ApiUrlCandidate.test(action.url)
        SourceManager.setFailOnce()
        RestartWorker.run(context, delay = 0)
    }

    private fun deleteRegistration(context: Context, action: Action.DeleteRegistration) {
        action.registrations.forEach {
            Distributor.deleteApp(context, it) {}
        }
    }
}

fun ViewModel.publishAction(action: AppAction) {
    viewModelScope.launch {
        EventBus.publish(action)
    }
}
