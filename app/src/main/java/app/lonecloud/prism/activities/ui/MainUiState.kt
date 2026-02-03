package app.lonecloud.prism.activities.ui

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?
)

data class MainUiState(
    val showDebugInfo: Boolean = false,
    val showPermissionDialog: Boolean = true,
    val showAppDetails: Boolean = false,
    val isLoadingEndpoint: Boolean = false,
    val currentEndpoint: String = "",
    val showAddAppDialog: Boolean = false,
    val installedApps: List<InstalledApp> = emptyList()
)
