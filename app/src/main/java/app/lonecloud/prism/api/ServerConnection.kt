/*
 * Copyright (C) 2024 p1gp1g
 * Modified by lone-cloud under AGPL v3.0
 *
 * Original work: https://codeberg.org/Sunup/android
 * Licensed under Apache License 2.0
 */

package app.lonecloud.prism.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import app.lonecloud.prism.BuildConfig
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.Distributor
import app.lonecloud.prism.Distributor.sendMessage
import app.lonecloud.prism.EncryptionKeyStore
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.R
import app.lonecloud.prism.api.data.ClientMessage
import app.lonecloud.prism.api.data.NotificationPayload
import app.lonecloud.prism.api.data.ServerMessage
import app.lonecloud.prism.callback.NetworkCallbackFactory
import app.lonecloud.prism.services.FgService
import app.lonecloud.prism.services.RestartWorker
import app.lonecloud.prism.services.SourceManager
import app.lonecloud.prism.utils.DescriptionParser
import app.lonecloud.prism.utils.HttpClientFactory
import app.lonecloud.prism.utils.ManualAppNotifications
import app.lonecloud.prism.utils.TAG
import app.lonecloud.prism.utils.WebPushDecryptor
import app.lonecloud.prism.utils.redactIdentifier
import app.lonecloud.prism.utils.redactUrl
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.unifiedpush.android.distributor.ChannelCreationStatus

class ServerConnection(private val context: Context, private val releaseLock: () -> Unit) : WebSocketListener() {

    private val store = PrismPreferences(context)

    fun start(): WebSocket {
        val url = ApiUrlCandidate.getTest() ?: store.apiUrl
        val uaid = store.uaid
        debugLog { "Connecting to ${redactUrl(url)} [uaid?=${uaid != null}]" }
        val request = Request.Builder()
            .url(url)
            .build()
        return HttpClientFactory.longLived.newWebSocket(request, this).also { ws ->
            SourceManager.newSource(ws)
            ClientMessage.Hello(uaid).send(ws)
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        SourceManager.setConnected(context, webSocket)
        releaseLock()
        debugLog { "onOpen: ${response.code}" }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val message = ServerMessage.deserialize(text) ?: run {
            debugLog { "Couldn't deserialize incoming server message" }
            return
        }
        debugLog { "New message: ${message::class.java.simpleName}" }
        lastEventDate = Calendar.getInstance()
        when (message) {
            is ServerMessage.Broadcast -> ignoreEvent()

            is ServerMessage.Hello -> onHello(webSocket, message)

            is ServerMessage.Notification -> onNotification(webSocket, message)

            ServerMessage.Ping -> onPing(webSocket)

            is ServerMessage.Register -> onRegister(message)

            is ServerMessage.Unregister -> onUnregister(webSocket, message)

            is ServerMessage.Urgency -> {
                debugLog { "Urgency status=${message.status}" }
            }
        }
    }

    private fun ignoreEvent() {
        debugLog { "Ignoring event" }
    }

    private fun onHello(webSocket: WebSocket, message: ServerMessage.Hello) {
        debugLog { "Hello" }
        SourceManager.debugStarted()
        ApiUrlCandidate.finish(context)?.let {
            store.apiUrl = it
            debugLog { "Successfully using ${redactUrl(it)}" }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_url_candidate_success, it),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        val db = DatabaseFactory.getDb(context)
        if (message.uaid != store.uaid) {
            debugLog { "We received a new uaid" }
            store.uaid = message.uaid
            db.listChannelIdVapid().forEach { pair ->
                ClientMessage.Register(
                    channelID = pair.first,
                    key = pair.second
                ).send(webSocket)
            }
            db.deleteDisabledApps()
        } else {
            db.listDisabledChannelIds().forEach {
                debugLog { "Hello, unregistering $it" }
                ClientMessage.Unregister(channelID = it).send(webSocket)
            }
            db.listPendingChannelIdVapid().forEach { pair ->
                debugLog { "Hello, registering channel=${redactIdentifier(pair.first)}" }
                ClientMessage.Register(
                    channelID = pair.first,
                    key = pair.second
                ).send(webSocket)
            }
        }
    }

    private fun decryptNotificationData(channelID: String, encryptedData: ByteArray): ByteArray {
        val keys = EncryptionKeyStore(context).getKeys(channelID) ?: run {
            debugLog { "No encryption keys found for manual channel=${redactIdentifier(channelID)}, message may be unencrypted" }
            return encryptedData
        }
        val publicKeyBytes = Base64.decode(keys.p256dh, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return try {
            WebPushDecryptor.decrypt(encryptedData, keys.privateKey, publicKeyBytes, keys.authSecret)
                ?: encryptedData.also { Log.e(TAG, "Decryption failed for channel=${redactIdentifier(channelID)}") }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Decryption error for channel=${redactIdentifier(channelID)}: ${e.message}")
            encryptedData
        }
    }

    private fun onNotification(webSocket: WebSocket, message: ServerMessage.Notification) {
        val encryptedData = Base64.decode(message.data, Base64.URL_SAFE)

        val db = DatabaseFactory.getDb(context)
        val vapidKey = db.listChannelIdVapid()
            .find { (channelId, _) -> channelId == message.channelID }
            ?.second

        val app = if (vapidKey != null) {
            db.listApps().find { it.vapidKey == vapidKey && DescriptionParser.isManualApp(it.description) }
        } else {
            null
        }

        val decryptedData = if (app != null) {
            decryptNotificationData(message.channelID, encryptedData)
        } else {
            encryptedData
        }

        if (app != null) {
            val dataString = String(decryptedData, Charsets.UTF_8)
            val payload = NotificationPayload.fromJson(dataString)

            if (payload != null) {
                debugLog { "Displaying notification for manual app '${app.title}': ${payload.title}" }
                ManualAppNotifications.showNotification(
                    context,
                    message.channelID,
                    app,
                    payload
                )
            } else {
                Log.w(TAG, "Failed to parse notification payload for manual app, falling back to sendMessage")
                sendMessage(context, message.channelID, decryptedData)
            }
        } else {
            sendMessage(context, message.channelID, decryptedData)
        }

        ClientMessage.Ack(
            arrayOf(ClientMessage.ClientAck(message.channelID, message.version))
        ).send(webSocket)
    }

    private fun onPing(webSocket: WebSocket) {
        SourceManager.debugNewPing(context)
        if (!waitingPong.getAndSet(false)) {
            debugLog { "Sending Pong" }
            ClientMessage.Ping.send(webSocket)
        } else {
            debugLog { "Received Pong" }
        }
    }

    private fun onRegister(message: ServerMessage.Register) {
        debugLog { "New endpoint received for channel=${redactIdentifier(message.channelID)}" }
        Distributor.finishRegistration(
            context,
            ChannelCreationStatus.Ok(message.channelID, message.pushEndpoint)
        )
        ManualAppRegistrationHandler(context).handleRegister(message)
    }

    private fun onUnregister(webSocket: WebSocket, message: ServerMessage.Unregister) {
        val db = DatabaseFactory.getDb(context)
        val channelVapidPair = db.listChannelIdVapid().find { (channelId, _) -> channelId == message.channelID }
        val appForChannel = channelVapidPair?.let { (_, vapid) ->
            db.listApps().find { app -> app.vapidKey == vapid }
        }
        val isManualChannel = appForChannel?.let { DescriptionParser.isManualApp(it.description) } == true

        if (isManualChannel) {
            val vapidKey = channelVapidPair.second
            if (PrismPreferences(context).isPendingChannelDeletion(message.channelID)) {
                debugLog { "Channel ${redactIdentifier(message.channelID)} is pending deletion, skipping re-registration" }
                PrismPreferences(context).removePendingChannelDeletion(message.channelID)
                return
            }
            Log.w(
                TAG,
                "Received unregister for manual channel ${redactIdentifier(message.channelID)}; re-registering instead of deleting app"
            )
            ClientMessage.Register(
                channelID = message.channelID,
                key = vapidKey
            ).send(webSocket)
            return
        }
        Distributor.deleteChannelFromServer(context, message.channelID)
    }

    override fun onClosed(
        webSocket: WebSocket,
        code: Int,
        reason: String
    ) {
        debugLog { "onClosed: $webSocket" }
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
        debugLog { "onFailure: An error occurred: $t" }
        response?.let {
            debugLog { "onFailure: ${it.code}" }
        }
        releaseLock()
        if (failToUseUrlCandidate(context)) return
        if (!shouldRestart()) return
        if (SourceManager.addFail(context, webSocket)) {
            val delay = SourceManager.getTimeout() ?: return
            debugLog { "Retrying in $delay ms" }
            RestartWorker.run(context, delay = delay)
        }
    }

    private fun failToUseUrlCandidate(context: Context): Boolean {
        ApiUrlCandidate.finish(context)?.let { url ->
            debugLog { "Fail to use ${redactUrl(url)}" }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_url_candidate_fail, url),
                    Toast.LENGTH_SHORT
                ).show()
            }
            RestartWorker.run(context, delay = 0)
            return true
        }
        return false
    }

    @Suppress("ReturnCount")
    private fun shouldRestart(): Boolean {
        if (!FgService.isServiceStarted()) {
            debugLog { "StartService not started" }
            return false
        }
        if (!NetworkCallbackFactory.hasInternet()) {
            debugLog { "No Internet: do not restart" }
            return false
        }
        return true
    }

    private inline fun debugLog(message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message())
        }
    }

    companion object {
        var lastEventDate: Calendar? = null
        var waitingPong = AtomicBoolean(false)
        fun destroy() = SourceManager.removeSource()
    }
}
