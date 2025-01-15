package org.unifiedpush.distributor.sunup.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.unifiedpush.android.distributor.ui.R as LibR
import org.unifiedpush.distributor.receiver.DistributorReceiver
import org.unifiedpush.distributor.sunup.AppStore
import org.unifiedpush.distributor.sunup.DatabaseFactory
import org.unifiedpush.distributor.sunup.Distributor
import org.unifiedpush.distributor.sunup.Distributor.sendMessage
import org.unifiedpush.distributor.sunup.activities.UiAction
import org.unifiedpush.distributor.sunup.api.data.ClientMessage
import org.unifiedpush.distributor.sunup.api.data.ServerMessage
import org.unifiedpush.distributor.sunup.callback.NetworkCallbackFactory
import org.unifiedpush.distributor.sunup.services.FailureCounter
import org.unifiedpush.distributor.sunup.services.FgService
import org.unifiedpush.distributor.sunup.services.RestartWorker
import org.unifiedpush.distributor.sunup.utils.TAG
import org.unifiedpush.distributor.utils.addOnce
import org.unifiedpush.distributor.utils.removeSync

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
        return client.newWebSocket(request, this).also {
            ClientMessage.Hello(uaid).send(it)
        }
    }

    override fun onOpen(ws: WebSocket, response: Response) {
        FailureCounter.newSource(context, ws)
        releaseLock()
        try {
            Log.d(TAG, "onOpen: " + response.code)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMessage(ws: WebSocket, text: String) {
        val message = ServerMessage.deserialize(text) ?: run {
            Log.d(TAG, "Couldn't deserialize $text")
        }
        Log.d(TAG, "New message: ${message::class.java.simpleName}")
        lastEventDate = Calendar.getInstance()
        when (message) {
            is ServerMessage.Broadcast -> ignoreEvent()
            is ServerMessage.Hello -> onHello(ws, message)
            is ServerMessage.Notification -> onNotification(ws, message)
            ServerMessage.Ping -> onPing(ws)
            is ServerMessage.Register -> onRegister(message)
            is ServerMessage.Unegister -> onUnregister(message)
            is ServerMessage.Urgency -> {
                Log.d(TAG, "Urgency status=${message.status}")
            }
        }
    }

    private fun ignoreEvent() {
        Log.d(TAG, "Ignoring event")
    }

    private fun onHello(ws: WebSocket, message: ServerMessage.Hello) {
        Log.d(TAG, "Hello")
        ApiUrlCandidate.finish()?.let {
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
        if (message.uaid != store.uaid) {
            Log.d(TAG, "We received a new uaid")
            store.uaid = message.uaid
            DatabaseFactory.getDb(context).listAppVapidTokens().forEach { pair ->
                ClientMessage.Register(
                    channelID = pair.first,
                    key = pair.second
                ).send(ws)
            }
        }
    }

    private fun onNotification(ws: WebSocket, message: ServerMessage.Notification) {
        sendMessage(
            context,
            message.channelID,
            Base64.decode(message.data, Base64.URL_SAFE)
        )
        ClientMessage.Ack(
            arrayOf(ClientMessage.ClientAck(message.channelID, message.version))
        ).send(ws)
    }

    private fun onPing(ws: WebSocket) {
        FailureCounter.newPing(context)
        if (!waitingPong.getAndSet(false)) {
            Log.d(TAG, "Sending Pong")
            ClientMessage.Ping.send(ws)
        } else {
            Log.d(TAG, "Received Pong")
        }
    }

    private fun onRegister(message: ServerMessage.Register) {
        Log.d(TAG, "New endpoint: ${message.pushEndpoint}")
        DatabaseFactory.getDb(context).saveEndpoint(message.channelID, message.pushEndpoint)
        Distributor.sendEndpointForChannel(context, message.channelID)
    }

    private fun onUnregister(message: ServerMessage.Unegister) {
        val token = DatabaseFactory.getDb(context).getConnectorToken(message.channelID)
        if (token != null && DistributorReceiver.delQueue.addOnce(token)) {
            Distributor.deleteAppFromServer(context, token)
            DistributorReceiver.delQueue.removeSync(token)
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "onClosed: $webSocket")
        webSocket.cancel()
        releaseLock()
        if (shouldRestart() && FailureCounter.addFail(context, webSocket)) {
            RestartWorker.run(context, delay = 0)
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d(TAG, "onFailure")
        webSocket.cancel()
        Log.d(TAG, "An error occurred: $t")
        response?.let {
            Log.d(TAG, "onFailure: ${it.code}")
        }
        releaseLock()
        if (failToUseUrlCandidate(context)) return
        if (!shouldRestart()) return
        if (FailureCounter.addFail(context, webSocket)) {
            // If null, we keep the worker with its 16min
            val delay = FailureCounter.getTimeout() ?: return
            Log.d(TAG, "Retrying in $delay ms")
            RestartWorker.run(context, delay = delay)
        }
    }

    private fun failToUseUrlCandidate(context: Context): Boolean {
        ApiUrlCandidate.finish()?.let { url ->
            Log.d(TAG, "Fail to use $url")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    context.getString(LibR.string.toast_url_candidate_fail, url),
                    Toast.LENGTH_SHORT
                ).show()
            }
            UiAction.publish(UiAction.Action.RefreshApiUrl)
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
    private fun shouldRestart(): Boolean {
        if (!FgService.isServiceStarted()) {
            Log.d(TAG, "StartService not started")
            return false
        }
        if (!NetworkCallbackFactory.hasInternet) {
            Log.d(TAG, "No Internet: do not restart")
            // It will be restarted when Internet is back
            return false
        }
        return true
    }

    companion object {
        var lastEventDate: Calendar? = null
        var waitingPong = AtomicBoolean(false)
    }
}
