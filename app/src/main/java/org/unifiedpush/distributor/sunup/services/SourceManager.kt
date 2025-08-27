package org.unifiedpush.distributor.sunup.services

import android.content.Context
import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString
import org.unifiedpush.distributor.AppNotification
import org.unifiedpush.distributor.SourceManager as SManager
import org.unifiedpush.distributor.sunup.utils.DisconnectedNotification

object SourceManager : SManager<WebSocket>() {
    override val foregroundService = FgService.service

    override fun disconnectedNotification(context: Context): AppNotification {
        return DisconnectedNotification(context)
    }

    override fun getDummySource(): WebSocket {
        return object : WebSocket {
            override fun cancel() {}
            override fun close(code: Int, reason: String?): Boolean {
                return true
            }
            override fun queueSize(): Long {
                return 0L
            }
            override fun request(): Request {
                return Request.Builder().build()
            }
            override fun send(text: String): Boolean {
                return true
            }
            override fun send(bytes: ByteString): Boolean {
                return true
            }
        }
    }

    override fun cancelSource(source: WebSocket?) {
        source?.cancel()
    }
}
