package app.lonecloud.prism.services

import android.content.Context
import app.lonecloud.prism.utils.DisconnectedNotification
import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString
import org.unifiedpush.distributor.AppNotification
import org.unifiedpush.distributor.SourceManager as SManager

object SourceManager : SManager<WebSocket>() {
    override val foregroundService = FgService.service
    override val migrationManager = MigrationManager()

    override fun disconnectedNotification(context: Context): AppNotification = DisconnectedNotification(context)

    override fun getDummySource(): WebSocket = object : WebSocket {
        override fun cancel() { /* Dummy implementation */ }
        override fun close(code: Int, reason: String?): Boolean = true
        override fun queueSize(): Long = 0L
        override fun request(): Request = Request.Builder().build()
        override fun send(text: String): Boolean = true
        override fun send(bytes: ByteString): Boolean = true
    }

    override fun cancelSource(source: WebSocket?) {
        source?.cancel()
    }
}
