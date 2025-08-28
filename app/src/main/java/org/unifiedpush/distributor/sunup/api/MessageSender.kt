package org.unifiedpush.distributor.sunup.api

import android.content.Context
import android.util.Log
import okhttp3.WebSocket
import org.unifiedpush.distributor.sunup.api.data.ClientMessage
import org.unifiedpush.distributor.sunup.services.RestartWorker
import org.unifiedpush.distributor.sunup.utils.TAG

object MessageSender {
    private var websocket: WebSocket? = null

    fun newWs(ws: WebSocket) {
        synchronized(this) {
            websocket = ws
        }
    }

    fun send(context: Context, message: ClientMessage) {
        synchronized(this) {
            websocket?.let {
                // Log.d(TAG, "Sending: ${message.serialize()}")
                Log.d(TAG, "Sending: ${message::class.java.simpleName}")
                message.send(it)
            } ?: run {
                Log.d(TAG, "Msg not sent, will be during restart")
                RestartWorker.run(context, delay = 0)
            }
        }
    }

    fun ping(context: Context) {
        send(context, ClientMessage.Ping)
        ServerConnection.waitingPong.set(true)
    }
}
