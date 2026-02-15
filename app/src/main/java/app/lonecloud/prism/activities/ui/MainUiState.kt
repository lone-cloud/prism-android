package app.lonecloud.prism.activities.ui

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?
)

data class MainUiState(
    val showPermissionDialog: Boolean = true,
    val showAppDetails: Boolean = false,
    val isLoadingEndpoint: Boolean = false,
    val currentEndpoint: String = "",
    val installedApps: List<InstalledApp> = emptyList(),
    val prismServerConfigured: Boolean = false
)
