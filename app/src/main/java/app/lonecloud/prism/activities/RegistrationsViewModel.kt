package app.lonecloud.prism.activities

import android.content.Context
import app.lonecloud.prism.DatabaseFactory
import org.unifiedpush.android.distributor.ui.compose.state.RegistrationListState
import org.unifiedpush.android.distributor.ui.compose.state.RegistrationState
import org.unifiedpush.distributor.Database

fun getRegistrationListState(context: Context): RegistrationListState = RegistrationListState(
    list = DatabaseFactory.getDb(context).listApps().map { app ->
        getRegistrationState(context, app)
    }
)

fun getRegistrationState(context: Context, app: Database.App): RegistrationState {
    // Parse manual app format: "target:package.name|optional_description"
    val isManualApp = app.description?.startsWith("target:") == true
    val targetPackage = app.description?.takeIf { isManualApp }
        ?.substringAfter("target:")?.substringBefore("|")
        ?.takeIf { it.isNotBlank() }

    val cleanDescription = if (isManualApp) {
        app.description?.substringAfter("|", "")?.takeIf { it.isNotBlank() }
    } else {
        app.description
    }

    val displayPackage = targetPackage ?: app.packageName
    val displayDescription = if (isManualApp && targetPackage == null) null else cleanDescription

    val baseAppState = context.applicationRowState(displayPackage, displayDescription)

    // For manual apps without target, use custom title instead of package-derived one
    val displayTitle = if (isManualApp && targetPackage == null) {
        app.title ?: baseAppState.title
    } else {
        baseAppState.title
    }

    val finalAppState = baseAppState.copy(
        title = displayTitle
    )

    return RegistrationState(
        app = finalAppState,
        msgCount = app.msgCount,
        token = app.connectorToken,
        copyable = false
    )
}
