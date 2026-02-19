package app.lonecloud.prism.activities

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.getValue
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
import app.lonecloud.prism.utils.DescriptionParser
import app.lonecloud.prism.utils.ManualAppNotifications
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
    private companion object {
        private const val VAPID_PRIVATE_DESC_PREFIX = "vp:"
    }

    constructor(requireBatteryOpt: Boolean, messenger: InternalMessenger?, application: Application) : this(
        mainUiState = MainUiState(
            prismServerConfigured = PrismPreferences(application).getPrismServerConfig() != null
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

    var selectedApp by mutableStateOf<InstalledApp?>(null)
        private set

    var selectedRegistrationToken by mutableStateOf<String?>(null)
        private set

    var prefilledName by mutableStateOf<String?>(null)

    fun updatePrismServerConfigured(configured: Boolean) {
        mainUiState = mainUiState.copy(prismServerConfigured = configured)
    }

    init {
        loadInstalledApps()
        application?.let { app ->
            ManualAppNotifications.pruneOrphanedChannels(app)
        }
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
                    if (token.startsWith("manual_app_")) {
                        PrismServerClient.deleteApp(app, token)
                        ManualAppNotifications.deleteChannelForToken(app, token)
                    }

                    val intent = android.content.Intent("org.unifiedpush.android.distributor.UNREGISTER")
                    intent.setPackage(app.packageName)
                    intent.putExtra("token", token)
                    app.sendBroadcast(intent)
                    PrismPreferences(app).removeVapidPrivateKey(token)
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

    fun isManualApp(token: String) = getApp(token)?.let { DescriptionParser.isManualApp(it.description) } == true

    fun getEndpoint(token: String): String? = getApp(token)?.endpoint

    fun getChannelId(token: String): String? = application?.let { app ->
        val db = DatabaseFactory.getDb(app)
        val appEntry = db.listApps().find { it.connectorToken == token } ?: return@let null
        db.listChannelIdVapid().find { (_, vapid) -> vapid == appEntry.vapidKey }?.first
    }

    fun hideAppDetails() {
        mainUiState = mainUiState.copy(showAppDetails = false)
    }

    fun selectApp(app: InstalledApp) {
        selectedApp = app
    }

    fun selectRegistration(token: String) {
        selectedRegistrationToken = token
    }

    fun clearSelectedRegistration() {
        selectedRegistrationToken = null
    }

    fun clearSelectedApp() {
        selectedApp = null
    }

    fun addManualApp(
        name: String,
        packageName: String,
        description: String?
    ) {
        addApp(name, packageName, description)
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

    fun addApp(
        name: String,
        targetPackageName: String,
        description: String?
    ) {
        viewModelScope.launch {
            application?.let { app ->
                val channelId = UUID.randomUUID().toString()
                val connectorToken = "manual_app_${UUID.randomUUID()}"

                Log.d(TAG, "Creating manual app: $name, token: $connectorToken")

                val vapidKeys = VapidKeyGenerator.generateKeyPair()
                val encryptionKeys = WebPushEncryptionKeys.generateKeySet()

                val descriptionParts = mutableListOf("target:$targetPackageName")
                description?.takeIf { it.isNotBlank() }?.let { descriptionParts.add(it) }
                descriptionParts.add("$VAPID_PRIVATE_DESC_PREFIX${vapidKeys.privateKey}")
                val fullDescription = descriptionParts.joinToString("|")

                val keyStore = EncryptionKeyStore(app)
                keyStore.storeKeys(channelId, encryptionKeys.privateKey, encryptionKeys.authBytes, encryptionKeys.p256dh)

                val packageName = targetPackageName.ifBlank { app.packageName }
                val preferences = PrismPreferences(app)
                preferences.addPendingManualToken(connectorToken)
                preferences.setVapidPrivateKey(connectorToken, vapidKeys.privateKey)

                val db = DatabaseFactory.getDb(app)
                db.registerApp(
                    packageName,
                    connectorToken,
                    channelId,
                    name,
                    vapidKeys.publicKey,
                    fullDescription
                )

                val intent = Intent("org.unifiedpush.android.distributor.REGISTER").apply {
                    `package` = app.packageName
                    putExtra("token", connectorToken)
                    putExtra("application", packageName)
                    putExtra("message", name)
                }
                app.sendBroadcast(intent)

                refreshRegistrations()
            }
        }
    }

    fun deleteRegistration(token: String) {
        viewModelScope.launch {
            application?.let { app ->
                if (token.startsWith("manual_app_")) {
                    PrismServerClient.deleteApp(app, token)
                    ManualAppNotifications.deleteChannelForToken(app, token)
                }

                val intent = Intent("org.unifiedpush.android.distributor.UNREGISTER")
                intent.setPackage(app.packageName)
                intent.putExtra("token", token)
                app.sendBroadcast(intent)
                PrismPreferences(app).removeVapidPrivateKey(token)
                if (selectedRegistrationToken == token) {
                    clearSelectedRegistration()
                }
                refreshRegistrations()
            }
        }
    }
}
