package app.lonecloud.prism.activities.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.lonecloud.prism.R

data class PrismServerApp(val name: String, val matchedInstalledApp: InstalledApp? = null)

@Composable
fun AppPickerScreen(
    apps: List<InstalledApp>,
    onNavigateBack: () -> Unit,
    onSelect: (InstalledApp) -> Unit,
    onSelectPrismApp: ((String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var prismServerApps by remember { mutableStateOf<List<PrismServerApp>>(emptyList()) }
    var prismAppsLoaded by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        showContent = true
    }

    val recommendedPackages = listOf(
        "ch.protonmail.android",
        "io.homeassistant.companion.android"
    )

    androidx.compose.runtime.LaunchedEffect(onSelectPrismApp) {
        if (onSelectPrismApp != null) {
            app.lonecloud.prism.PrismServerClient.fetchRegisteredApps(
                context,
                onSuccess = { serverAppNames ->
                    val db = app.lonecloud.prism.DatabaseFactory.getDb(context)
                    val localAppNames = db.listApps()
                        .filter { it.description?.startsWith("target:") == true }
                        .mapNotNull { it.title }
                        .toSet()
                    prismServerApps = serverAppNames
                        .filterNot { it in localAppNames }
                        .map { serverAppName ->
                            val matchedApp = apps.find {
                                it.appName.equals(serverAppName, ignoreCase = true)
                            }
                            PrismServerApp(serverAppName, matchedApp)
                        }
                    prismAppsLoaded = true
                },
                onError = {
                    prismAppsLoaded = true
                }
            )
        } else {
            prismAppsLoaded = true
        }
    }

    val (recommendedApps, otherApps) = remember(apps, searchQuery, prismServerApps) {
        val filtered = if (searchQuery.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

        val prismAppNames = prismServerApps.map { it.name }
        val recommended = filtered.filter { app ->
            recommendedPackages.any { pkg -> app.packageName.startsWith(pkg) } &&
                !prismAppNames.contains(app.appName)
        }.sortedBy { it.appName.lowercase() }
        val others = filtered.filterNot { app ->
            recommendedPackages.any { pkg -> app.packageName.startsWith(pkg) }
        }.sortedBy { it.appName.lowercase() }

        recommended to others
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(stringResource(R.string.search_apps_label)) },
            placeholder = { Text(stringResource(R.string.search_apps_placeholder)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showContent && prismAppsLoaded && prismServerApps.isNotEmpty() && onSelectPrismApp != null && searchQuery.isBlank()) {
                item {
                    Text(
                        text = stringResource(R.string.from_your_server),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }
                items(prismServerApps) { prismApp ->
                    PrismAppListItem(
                        prismApp = prismApp,
                        onClick = {
                            if (prismApp.matchedInstalledApp != null) {
                                onSelect(prismApp.matchedInstalledApp)
                                onNavigateBack()
                            } else {
                                searchQuery = prismApp.name
                            }
                        }
                    )
                }
            }

            if (showContent && recommendedApps.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.recommended_apps),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }
                items(recommendedApps) { app ->
                    AppListItem(
                        app = app,
                        isRecommended = true,
                        onClick = {
                            onSelect(app)
                            onNavigateBack()
                        }
                    )
                }
            }

            if (showContent && otherApps.isNotEmpty()) {
                if (recommendedApps.isNotEmpty() || prismServerApps.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.all_apps),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }
                }
                items(otherApps) { app ->
                    AppListItem(
                        app = app,
                        isRecommended = false,
                        onClick = {
                            onSelect(app)
                            onNavigateBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: InstalledApp,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let { icon ->
            val bitmap = icon.toBitmap(48, 48)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }
        Column {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrismAppListItem(prismApp: PrismServerApp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = prismApp.matchedInstalledApp?.icon
        if (icon != null) {
            val bitmap = icon.toBitmap(48, 48)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }
        Text(
            text = prismApp.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
