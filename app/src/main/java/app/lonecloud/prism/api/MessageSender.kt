/*
 * Copyright (C) 2024 p1gp1g
 * Modified by lone-cloud under AGPL v3.0
 *
 * Original work: https://codeberg.org/Sunup/android
 * Licensed under Apache License 2.0
 */

package app.lonecloud.prism.api

import android.content.Context
import android.util.Log
import app.lonecloud.prism.api.data.ClientMessage
import app.lonecloud.prism.services.RestartWorker
import app.lonecloud.prism.utils.TAG
import java.util.Calendar
import okhttp3.WebSocket

object MessageSender {
    private var websocket: WebSocket? = null
    private var lastPing: Calendar? = null

    fun newWs(ws: WebSocket) {
        synchronized(this) {
            websocket = ws
            lastPing = null
        }
    }

    fun send(context: Context, message: ClientMessage) {
        synchronized(this) {
            websocket?.let {
                Log.d(TAG, "Sending: ${message::class.java.simpleName}")
                message.send(it)
            } ?: run {
                Log.d(TAG, "Msg not sent, will be during restart")
                try {
                    RestartWorker.run(context, delay = 0)
                } catch (e: IllegalStateException) {
                    Log.d(TAG, "WorkManager not available in this process, service will handle restart", e)
                }
            }
        }
    }

    /**
     * Send a ping to the server if we haven't done it in the last 60 seconds.
     * The server silently closes the connection if pings are sent too frequently.
     */
    fun ping(context: Context) {
        val now = Calendar.getInstance()
        val sendPing = lastPing?.let { last ->
            val threshold = Calendar.getInstance().also {
                it.time = last.time
                it.add(Calendar.SECOND, 60)
            }
            now.after(threshold)
        } ?: true
        if (sendPing) {
            send(context, ClientMessage.Ping)
            ServerConnection.waitingPong.set(true)
            lastPing = now
        }
    }
}
