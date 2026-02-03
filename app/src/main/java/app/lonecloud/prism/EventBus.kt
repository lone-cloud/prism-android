package app.lonecloud.prism

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance

object EventBus {
    val mutEvents: MutableSharedFlow<Any> = MutableSharedFlow()
    val events = mutEvents.asSharedFlow()

    suspend inline fun <reified T : Any> publish(event: T) {
        if (mutEvents.subscriptionCount.value > 0) {
            mutEvents.emit(event)
        }
    }

    suspend inline fun <reified T> subscribe(crossinline onEvent: (T) -> Unit) {
        events.filterIsInstance<T>()
            .collectLatest { event ->
                coroutineContext.ensureActive()
                onEvent(event)
            }
    }
}
