package org.unifiedpush.distributor.sunup.activities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.unifiedpush.distributor.sunup.EventBus

class UiAction(val action: Action) {
    enum class Action {
        RefreshRegistrations,
        RefreshApiUrl
    }

    fun handle(action: (Action) -> Unit) {
        action(this.action)
    }

    companion object {
        fun publish(type: Action) {
            CoroutineScope(Dispatchers.IO).launch {
                EventBus.publish(UiAction(type))
            }
        }
    }
}
