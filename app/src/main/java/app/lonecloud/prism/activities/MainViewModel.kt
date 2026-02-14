package app.lonecloud.prism.activities

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lonecloud.prism.DatabaseFactory
import app.lonecloud.prism.EncryptionKeyStore
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.PrismServerClient
import app.lonecloud.prism.activities.ui.InstalledApp
import app.lonecloud.prism.activities.ui.MainUiState
import app.lonecloud.prism.api.MessageSender
import app.lonecloud.prism.api.data.ClientMessage
import app.lonecloud.prism.utils.TAG
import app.lonecloud.prism.utils.VapidKeyGenerator
import app.lonecloud.prism.utils.WebPushEncryptionKeys
import java.util.UUID
import kotlinx.coroutines.launch
import org.unifiedpush.android.distributor.data.App
import org.unifiedpush.android.distributor.ipc.InternalMessenger
import org.unifiedpush.android.distributor.ipc.InternalOpcode
import org.unifiedpush.android.distributor.ui.state.RegistrationListState
import org.unifiedpush.android.distributor.ui.vm.BatteryOptimisationViewModel
import org.unifiedpush.android.distributor.ui.vm.RegistrationsViewModel

class MainViewModel(
    mainUiState: MainUiState,
    val batteryOptimisationViewModel: BatteryOptimisationViewModel,
    val registrationsViewModel: RegistrationsViewModel,
    val messenger: InternalMessenger?,
    val application: Application? = null
) : ViewModel() {
    constructor(requireBatteryOpt: Boolean, messenger: InternalMessenger?, application: Application) : this(
        mainUiState = MainUiState(
            prismServerConfigured = !PrismPreferences(application).prismServerUrl.isNullOrBlank() &&
                !PrismPreferences(application).prismApiKey.isNullOrBlank()
        ),
        batteryOptimisationViewModel = BatteryOptimisationViewModel(requireBatteryOpt, messenger),
        registrationsViewModel = RegistrationsViewModel(
            RegistrationListState(emptyList<App>()),
            messenger
        ),
        messenger,
        application
    )

    var mainUiState by mutableStateOf(mainUiState)

    fun updatePrismServerConfigured(configured: Boolean) {
        mainUiState = mainUiState.copy(prismServerConfigured = configured)
    }

    private var lastDebugClickTime by mutableLongStateOf(0L)

    private var debugClickCount by mutableIntStateOf(0)

    init {
        loadInstalledApps()
    }

    fun closePermissionDialog() {
        viewModelScope.launch {
            mainUiState = mainUiState.copy(showPermissionDialog = false)
        }
    }

    fun refreshRegistrations() {
        viewModelScope.launch {
            val apps = messenger?.sendIMessageL(InternalOpcode.REG_LIST, "apps", App::class.java)
            registrationsViewModel.state = RegistrationListState(apps ?: emptyList())
        }
    }

    fun deleteSelection() {
        viewModelScope.launch {
            Log.d(TAG, "deleteSelection called")
            application?.let { app ->
                val selectedTokens = registrationsViewModel.state.list
                    .filter { it.selected }
                    .map { it.token }

                Log.d(TAG, "Deleting ${selectedTokens.size} apps: $selectedTokens")

                selectedTokens.forEach { token ->
                    val intent = android.content.Intent("org.unifiedpush.android.distributor.UNREGISTER")
                    intent.setPackage(app.packageName)
                    intent.putExtra("token", token)
                    app.sendBroadcast(intent)
                }

                registrationsViewModel.unselectAll()
            }
        }
    }

    private fun getApp(token: String) = application?.let { app ->
        DatabaseFactory.getDb(app).listApps().find { it.connectorToken == token }
    }

    fun showAppDetails(token: String) {
        viewModelScope.launch {
            getApp(token)?.let { appData ->
                mainUiState = mainUiState.copy(
                    showAppDetails = true,
                    currentEndpoint = appData.endpoint ?: "No endpoint available"
                )
            }
        }
    }

    fun isManualApp(token: String) = getApp(token)?.description?.startsWith("target:") == true

    fun hideAppDetails() {
        mainUiState = mainUiState.copy(showAppDetails = false)
    }

    fun addDebugClick() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDebugClickTime < 500) {
            debugClickCount++
            if (debugClickCount == 5) {
                mainUiState = mainUiState.copy(showDebugInfo = true)
            }
        } else {
            debugClickCount = 1
        }
        lastDebugClickTime = currentTime
    }

    fun dismissDebugInfo() {
        mainUiState = mainUiState.copy(showDebugInfo = false)
    }

    fun restartService() {
        viewModelScope.launch {
            messenger?.sendIMessage(InternalOpcode.WORKER_RESTART, 0)
        }
    }

    private fun hasUnifiedPushSupport(pm: PackageManager, packageName: String): Boolean {
        val intents = listOf("NEW_ENDPOINT", "MESSAGE").map {
            Intent("org.unifiedpush.android.connector.$it").apply { setPackage(packageName) }
        }
        return intents.any { pm.queryBroadcastReceivers(it, PackageManager.MATCH_ALL).isNotEmpty() }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            application?.let { app ->
                val pm = app.packageManager
                val prismPackageName = app.packageName
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filterNot { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 }
                    .filterNot { hasUnifiedPushSupport(pm, it.packageName) }
                    .map { appInfo ->
                        InstalledApp(
                            packageName = appInfo.packageName,
                            appName = appInfo.loadLabel(pm).toString(),
                            icon = appInfo.loadIcon(pm)
                        )
                    }
                    .sortedWith(
                        compareBy(
                            { it.packageName != prismPackageName },
                            { it.appName }
                        )
                    )

                mainUiState = mainUiState.copy(installedApps = apps)
            }
        }
    }

    fun showAddAppDialog() {
        mainUiState = mainUiState.copy(showAddAppDialog = true)
    }

    fun hideAddAppDialog() {
        mainUiState = mainUiState.copy(showAddAppDialog = false)
    }

    fun addApp(
        name: String,
        targetPackageName: String,
        description: String?
    ) {
        viewModelScope.launch {
            application?.let { app ->
                val channelId = UUID.randomUUID().toString()
                val connectorToken = "manual_app_${UUID.randomUUID()}"
                val fullDescription = "target:$targetPackageName${description?.let { "|$it" } ?: ""}"

                Log.d(TAG, "Creating manual app: $name, token: $connectorToken")

                val vapidKeys = VapidKeyGenerator.generateKeyPair()
                val encryptionKeys = WebPushEncryptionKeys.generateKeySet()

                val keyStore = EncryptionKeyStore(app)
                keyStore.storeKeys(channelId, encryptionKeys.privateKey, encryptionKeys.authBytes, encryptionKeys.p256dh)

                val db = DatabaseFactory.getDb(app)
                db.registerApp(
                    app.packageName,
                    connectorToken,
                    channelId,
                    name,
                    vapidKeys.publicKey,
                    fullDescription
                )

                MessageSender.send(
                    app,
                    ClientMessage.Register(
                        channelID = channelId,
                        key = vapidKeys.publicKey
                    )
                )

                refreshRegistrations()
                hideAddAppDialog()

                var endpoint: String?
                var attempts = 0
                repeat(60) {
                    kotlinx.coroutines.delay(500)
                    attempts++
                    endpoint = db.getEndpoint(connectorToken)
                    if (attempts % 10 == 0) {
                        Log.d(TAG, "Polling attempt $attempts/60, endpoint: ${endpoint ?: "null"}")
                    }
                    if (endpoint != null) {
                        Log.d(TAG, "Endpoint received after $attempts attempts: $endpoint")
                        PrismServerClient.registerApp(
                            app,
                            name,
                            endpoint!!,
                            vapidPrivateKey = vapidKeys.privateKey,
                            p256dh = encryptionKeys.p256dh,
                            auth = encryptionKeys.auth
                        )
                        return@launch
                    }
                }

                Log.e(TAG, "Endpoint timeout after 30 seconds for token: $connectorToken")
            }
        }
    }
}
