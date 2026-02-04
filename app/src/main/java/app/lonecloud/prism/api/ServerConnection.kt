package app.lonecloud.prism.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import app.lonecloud.prism.AppStore
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.Distributor
import app.lonecloud.prism.Distributor.sendMessage
import app.lonecloud.prism.EncryptionKeyStore
import app.lonecloud.prism.api.data.ClientMessage
import app.lonecloud.prism.api.data.ServerMessage
import app.lonecloud.prism.callback.NetworkCallbackFactory
import app.lonecloud.prism.services.FgService
import app.lonecloud.prism.services.RestartWorker
import app.lonecloud.prism.services.SourceManager
import app.lonecloud.prism.utils.TAG
import app.lonecloud.prism.utils.WebPushDecryptor
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.unifiedpush.android.distributor.ui.R as LibR
import org.unifiedpush.distributor.ChannelCreationStatus

class ServerConnection(private val context: Context, private val releaseLock: () -> Unit) : WebSocketListener() {

    private val store = AppStore(context)

    fun start(): WebSocket {
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(1, TimeUnit.MINUTES)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
        val url = ApiUrlCandidate.getTest() ?: store.apiUrl
        val uaid = store.uaid
        Log.d(TAG, "Connecting to $url [uaid?=${uaid != null}]")
        val request = Request.Builder()
            .url(url)
            .build()
        return client.newWebSocket(request, this).also { ws ->
            SourceManager.newSource(ws)
            ClientMessage.Hello(uaid).send(ws)
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        SourceManager.setConnected(context, webSocket)
        releaseLock()
        Log.d(TAG, "onOpen: " + response.code)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val message = ServerMessage.deserialize(text) ?: run {
            Log.d(TAG, "Couldn't deserialize $text")
        }
        Log.d(TAG, "New message: ${message::class.java.simpleName}")
        lastEventDate = Calendar.getInstance()
        when (message) {
            is ServerMessage.Broadcast -> ignoreEvent()
            is ServerMessage.Hello -> onHello(webSocket, message)
            is ServerMessage.Notification -> onNotification(webSocket, message)
            ServerMessage.Ping -> onPing(webSocket)
            is ServerMessage.Register -> onRegister(message)
            is ServerMessage.Unregister -> onUnregister(message)
            is ServerMessage.Urgency -> {
                Log.d(TAG, "Urgency status=${message.status}")
            }
        }
    }

    private fun ignoreEvent() {
        Log.d(TAG, "Ignoring event")
    }

    private fun onHello(webSocket: WebSocket, message: ServerMessage.Hello) {
        Log.d(TAG, "Hello")
        SourceManager.debugStarted()
        ApiUrlCandidate.finish(context)?.let {
            store.apiUrl = it
            Log.d(TAG, "Successfully using $it")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    context.getString(LibR.string.toast_url_candidate_success, it),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        val db = DatabaseFactory.getDb(context)
        if (message.uaid != store.uaid) {
            Log.d(TAG, "We received a new uaid")
            store.uaid = message.uaid
            db.listChannelIdVapid().forEach { pair ->
                ClientMessage.Register(
                    channelID = pair.first,
                    key = pair.second
                ).send(webSocket)
            }
            db.deleteDisabledApps()
        } else {
            // We remove pending unregistrations
            // and register pending registrations
            db.listDisabledChannelIds().forEach {
                Log.d(TAG, "Hello, unregistering $it")
                ClientMessage.Unregister(channelID = it).send(webSocket)
            }
            db.listPendingChannelIdVapid().forEach { pair ->
                Log.d(TAG, "Hello, registering: ${pair.first}")
                ClientMessage.Register(
                    channelID = pair.first,
                    key = pair.second
                ).send(webSocket)
            }
        }
    }

    private fun onNotification(webSocket: WebSocket, message: ServerMessage.Notification) {
        val encryptedData = Base64.decode(message.data, Base64.URL_SAFE)

        val db = DatabaseFactory.getDb(context)
        val app = db.listApps().find { app ->
            app.description?.startsWith("target:") == true
        }

        val decryptedData = if (app != null) {
            val keyStore = EncryptionKeyStore(context)
            val keys = keyStore.getKeys(message.channelID)

            if (keys != null) {
                val (privateKeyBytes, authSecret, publicKey) = keys

                try {
                    val publicKeyBytes = Base64.decode(publicKey, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

                    val decrypted = WebPushDecryptor.decrypt(
                        encryptedData,
                        privateKeyBytes,
                        publicKeyBytes,
                        authSecret
                    )

                    decrypted ?: run {
                        Log.e(TAG, "Decryption failed for channel ${message.channelID}")
                        encryptedData
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Decryption error for channel ${message.channelID}: ${e.message}")
                    encryptedData
                }
            } else {
                Log.d(TAG, "No encryption keys found for manual app ${message.channelID}, message may be unencrypted")
                encryptedData
            }
        } else {
            encryptedData
        }

        sendMessage(
            context,
            message.channelID,
            decryptedData
        )
        ClientMessage.Ack(
            arrayOf(ClientMessage.ClientAck(message.channelID, message.version))
        ).send(webSocket)
    }

    private fun onPing(webSocket: WebSocket) {
        SourceManager.debugNewPing(context)
        if (!waitingPong.getAndSet(false)) {
            Log.d(TAG, "Sending Pong")
            ClientMessage.Ping.send(webSocket)
        } else {
            Log.d(TAG, "Received Pong")
        }
    }

    private fun onRegister(message: ServerMessage.Register) {
        Log.d(TAG, "New endpoint: ${message.pushEndpoint}")
        Distributor.finishRegistration(
            context,
            ChannelCreationStatus.Ok(message.channelID, message.pushEndpoint)
        )
    }

    private fun onUnregister(message: ServerMessage.Unregister) {
        Distributor.deleteChannelFromServer(context, message.channelID)
    }

    override fun onClosed(
        webSocket: WebSocket,
        code: Int,
        reason: String
    ) {
        Log.d(TAG, "onClosed: $webSocket")
        webSocket.cancel()
        releaseLock()
        if (shouldRestart() && SourceManager.addFail(context, webSocket)) {
            RestartWorker.run(context, delay = 0)
        }
    }

    @Suppress("ReturnCount")
    override fun onFailure(
        webSocket: WebSocket,
        t: Throwable,
        response: Response?
    ) {
        Log.d(TAG, "onFailure: An error occurred: $t")
        response?.let {
            Log.d(TAG, "onFailure: ${it.code}")
        }
        releaseLock()
        if (failToUseUrlCandidate(context)) return
        if (!shouldRestart()) return
        if (SourceManager.addFail(context, webSocket)) {
            // If null, we keep the worker with its 16min
            val delay = SourceManager.getTimeout() ?: return
            Log.d(TAG, "Retrying in $delay ms")
            RestartWorker.run(context, delay = delay)
        }
    }

    private fun failToUseUrlCandidate(context: Context): Boolean {
        ApiUrlCandidate.finish(context)?.let { url ->
            Log.d(TAG, "Fail to use $url")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    context.getString(LibR.string.toast_url_candidate_fail, url),
                    Toast.LENGTH_SHORT
                ).show()
            }
            RestartWorker.run(context, delay = 0)
            return true
        }
        return false
    }

    /**
     * Check if service is started and if there is internet if the service has not started.
     *
     * @return [FgService.isServiceStarted]
     */
    @Suppress("ReturnCount")
    private fun shouldRestart(): Boolean {
        if (!FgService.isServiceStarted()) {
            Log.d(TAG, "StartService not started")
            return false
        }
        if (!NetworkCallbackFactory.hasInternet()) {
            Log.d(TAG, "No Internet: do not restart")
            // It will be restarted when Internet is back
            return false
        }
        return true
    }

    companion object {
        var lastEventDate: Calendar? = null
        var waitingPong = AtomicBoolean(false)
        fun destroy() = SourceManager.removeSource()
    }
}
