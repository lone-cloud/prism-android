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
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.Distributor
import app.lonecloud.prism.Distributor.sendMessage
import app.lonecloud.prism.EncryptionKeyStore
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.PrismServerClient
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
import app.lonecloud.prism.utils.WebPushEncryptionKeys
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.unifiedpush.android.distributor.ChannelCreationStatus
import org.unifiedpush.android.distributor.ipc.sendUiAction

class ServerConnection(private val context: Context, private val releaseLock: () -> Unit) : WebSocketListener() {

    private val store = PrismPreferences(context)

    fun start(): WebSocket {
        val url = ApiUrlCandidate.getTest() ?: store.apiUrl
        val uaid = store.uaid
        Log.d(TAG, "Connecting to $url [uaid?=${uaid != null}]")
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
            is ServerMessage.Unregister -> onUnregister(webSocket, message)
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
                    context.getString(R.string.toast_url_candidate_success, it),
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
        val vapidKey = db.listChannelIdVapid()
            .find { (channelId, _) -> channelId == message.channelID }
            ?.second

        val app = if (vapidKey != null) {
            db.listApps().find { it.vapidKey == vapidKey && DescriptionParser.isManualApp(it.description) }
        } else {
            null
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
                } catch (e: IllegalArgumentException) {
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

        if (app != null) {
            val dataString = String(decryptedData, Charsets.UTF_8)
            val payload = NotificationPayload.fromJson(dataString)

            if (payload != null) {
                Log.d(TAG, "Displaying notification for manual app '${app.title}': ${payload.title}")
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

        val db = DatabaseFactory.getDb(context)
        val vapidKey = db.listChannelIdVapid()
            .find { (channelId, _) -> channelId == message.channelID }
            ?.second

        val app = if (vapidKey != null) {
            db.listApps().find { it.vapidKey == vapidKey && DescriptionParser.isManualApp(it.description) }
        } else {
            null
        }

        if (app != null) {
            Log.d(TAG, "Auto-registering manual app '${app.title}' with Prism server")
            val vapidPrivateKey = DescriptionParser.extractValue(app.description, VAPID_PRIVATE_DESC_PREFIX)
                ?: PrismPreferences(context).getVapidPrivateKey(app.connectorToken)
            if (vapidPrivateKey.isNullOrBlank()) {
                handleManualRegistrationFailure(
                    appTitle = app.title,
                    connectorToken = app.connectorToken,
                    error = "Missing VAPID private key for ${app.title ?: "app"}. Delete and re-add the app."
                )
                return
            }

            val keyStore = EncryptionKeyStore(context)
            var keys = keyStore.getKeys(message.channelID)

            if (keys == null) {
                Log.w(TAG, "Missing encryption keys for channel ${message.channelID}, regenerating")
                val regenerated = WebPushEncryptionKeys.generateKeySet()
                keyStore.storeKeys(
                    message.channelID,
                    regenerated.privateKey,
                    regenerated.authBytes,
                    regenerated.p256dh
                )
                keys = keyStore.getKeys(message.channelID)
            }

            if (keys == null) {
                handleManualRegistrationFailure(
                    appTitle = app.title,
                    connectorToken = app.connectorToken,
                    error = "Failed to persist encryption keys for channel ${message.channelID}"
                )
                return
            }

            PrismServerClient.registerApp(
                context,
                app.connectorToken,
                app.title ?: "Unknown App",
                message.pushEndpoint,
                vapidPrivateKey = vapidPrivateKey,
                p256dh = keys.third,
                auth = android.util.Base64.encodeToString(
                    keys.second,
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
                ),
                onSuccess = {
                    Log.d(TAG, "Successfully registered '${app.title}' with Prism server")
                    PrismPreferences(context).removePendingManualToken(app.connectorToken)
                    sendUiAction(context, "RefreshRegistrations")
                },
                onError = { error ->
                    handleManualRegistrationFailure(
                        appTitle = app.title,
                        connectorToken = app.connectorToken,
                        error = error
                    )
                }
            )
        } else {
            Log.d(TAG, "Not a manual app or app not found for channelId: ${message.channelID}")
        }
    }

    private fun handleManualRegistrationFailure(
        appTitle: String?,
        connectorToken: String,
        error: String
    ) {
        Log.e(TAG, "Failed to register '$appTitle' with Prism server: $error")

        val preferences = PrismPreferences(context)
        val isInitialPendingRegistration = preferences.isPendingManualToken(connectorToken)

        if (isInitialPendingRegistration) {
            Log.w(
                TAG,
                "Rolling back pending manual app '$appTitle' after registration failure"
            )
            rollbackPendingManualAppRegistration(connectorToken)
        }

        preferences.removePendingManualToken(connectorToken)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Failed to register ${appTitle ?: "app"}: $error",
                Toast.LENGTH_LONG
            ).show()
        }

        sendUiAction(context, "RefreshRegistrations")
    }

    private fun rollbackPendingManualAppRegistration(connectorToken: String) {
        val db = DatabaseFactory.getDb(context)
        val app = db.listApps().find { it.connectorToken == connectorToken }
        val channelId = app?.let {
            db.listChannelIdVapid().find { (_, vapid) -> vapid == it.vapidKey }?.first
        }

        channelId?.let { EncryptionKeyStore(context).deleteKeys(it) }
        db.unregisterApp(connectorToken)

        val preferences = PrismPreferences(context)
        preferences.removeSubscriptionId(connectorToken)
        preferences.removeRegisteredEndpoint(connectorToken)
        preferences.removeVapidPrivateKey(connectorToken)
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
            Log.w(TAG, "Received unregister for manual channel ${message.channelID}; re-registering instead of deleting app")
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
                    context.getString(R.string.toast_url_candidate_fail, url),
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
            return false
        }
        return true
    }

    companion object {
        private const val VAPID_PRIVATE_DESC_PREFIX = "vp:"
        var lastEventDate: Calendar? = null
        var waitingPong = AtomicBoolean(false)
        fun destroy() = SourceManager.removeSource()
    }
}
