package org.unifiedpush.distributor.sunup.api

import android.content.Context
import okhttp3.WebSocket
import org.unifiedpush.distributor.sunup.api.data.ClientMessage
import org.unifiedpush.distributor.sunup.services.RestartWorker

object MessageSender {
    private var websocket: WebSocket? = null
    private val messageQueue = mutableSetOf<ClientMessage>()

    fun newWs(ws: WebSocket) {
        synchronized(this) {
            websocket = ws
        }
        messageQueue.removeAll {
            it.send(ws)
            true
        }
    }

    fun send(context: Context, message: ClientMessage) {
        synchronized(this) {
            websocket?.let {
                message.send(it)
            } ?: run {
                messageQueue.add(message)
                RestartWorker.run(context, delay = 0)
            }
        }
    }

    fun hasPendingMsgs(): Boolean {
        return messageQueue.isNotEmpty()
    }
}
