package app.lonecloud.prism.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.EncryptionKeyStore
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.PrismServerClient
import app.lonecloud.prism.R
import app.lonecloud.prism.api.data.ServerMessage
import app.lonecloud.prism.utils.DescriptionParser
import app.lonecloud.prism.utils.TAG
import app.lonecloud.prism.utils.UiActions
import app.lonecloud.prism.utils.WebPushEncryptionKeys
import app.lonecloud.prism.utils.redactIdentifier
import app.lonecloud.prism.utils.toBase64Url
import org.unifiedpush.android.distributor.ipc.sendUiAction

class ManualAppRegistrationHandler(private val context: Context) {

    fun handleRegister(message: ServerMessage.Register) {
        val db = DatabaseFactory.getDb(context)
        val vapidKey = db.listChannelIdVapid()
            .find { (channelId, _) -> channelId == message.channelID }
            ?.second

        val app = if (vapidKey != null) {
            db.listApps().find { it.vapidKey == vapidKey && DescriptionParser.isManualApp(it.description) }
        } else {
            null
        }

        if (app == null) {
            Log.d(TAG, "Not a manual app or app not found for channelId=${redactIdentifier(message.channelID)}")
            return
        }

        Log.d(TAG, "Auto-registering manual app '${app.title}' with Prism server")
        val vapidPrivateKey = DescriptionParser.extractValue(app.description, DescriptionParser.VAPID_PRIVATE_KEY_PREFIX)
            ?: PrismPreferences(context).getVapidPrivateKey(app.connectorToken)

        if (vapidPrivateKey.isNullOrBlank()) {
            handleFailure(
                appTitle = app.title,
                connectorToken = app.connectorToken,
                error = "Missing VAPID private key for ${app.title ?: "app"}. Delete and re-add the app."
            )
            return
        }

        val keyStore = EncryptionKeyStore(context)
        var keys = keyStore.getKeys(message.channelID)

        if (keys == null) {
            Log.w(TAG, "Missing encryption keys for channel=${redactIdentifier(message.channelID)}, regenerating")
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
            handleFailure(
                appTitle = app.title,
                connectorToken = app.connectorToken,
                error = "Failed to persist encryption keys for channel ${redactIdentifier(message.channelID)}"
            )
            return
        }

        PrismServerClient.registerApp(
            context,
            PrismServerClient.WebPushRegistration(
                connectorToken = app.connectorToken,
                appName = app.title ?: "Unknown App",
                webpushUrl = message.pushEndpoint,
                vapidPrivateKey = vapidPrivateKey,
                p256dh = keys.p256dh,
                auth = keys.authSecret.toBase64Url()
            ),
            onSuccess = {
                onRegistrationSuccess(app.title, app.connectorToken)
            },
            onError = { error ->
                handleFailure(
                    appTitle = app.title,
                    connectorToken = app.connectorToken,
                    error = error
                )
            }
        )
    }

    private fun onRegistrationSuccess(appTitle: String?, connectorToken: String) {
        Log.d(TAG, "Successfully registered '$appTitle' with Prism server")
        PrismPreferences(context).removePendingManualToken(connectorToken)
        sendUiAction(context, UiActions.RefreshRegistrations)
    }

    fun handleFailure(
        appTitle: String?,
        connectorToken: String,
        error: String
    ) {
        Log.e(TAG, "Failed to register '$appTitle' with Prism server: $error")

        val preferences = PrismPreferences(context)
        val isInitialPendingRegistration = preferences.isPendingManualToken(connectorToken)
        val isLikelyNewManualApp = isLikelyNewManualApp(connectorToken)
        val shouldRollback = isInitialPendingRegistration || isLikelyNewManualApp

        if (shouldRollback) {
            Log.w(TAG, "Rolling back pending manual app '$appTitle' after registration failure")
            rollback(connectorToken)
        }

        preferences.removePendingManualToken(connectorToken)

        Handler(Looper.getMainLooper()).post {
            val safeAppName = appTitle ?: context.getString(R.string.manual_app_generic_name)
            Toast.makeText(
                context,
                context.getString(R.string.manual_app_registration_failed, safeAppName, error),
                Toast.LENGTH_LONG
            ).show()
        }

        sendUiAction(context, UiActions.RefreshRegistrations)
    }

    private fun isLikelyNewManualApp(connectorToken: String): Boolean {
        val app = DatabaseFactory.getDb(context)
            .listApps()
            .find { it.connectorToken == connectorToken }
            ?: return false
        return DescriptionParser.isManualApp(app.description) && app.endpoint.isNullOrBlank()
    }

    private fun rollback(connectorToken: String) {
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
}
