package app.lonecloud.prism.activities

import app.lonecloud.prism.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UiAction(val action: Action) {
    enum class Action {
        RefreshRegistrations,
        UpdatePrismServerConfigured
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
