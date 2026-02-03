package app.lonecloud.prism.activities

import android.content.Context
import org.unifiedpush.android.distributor.ui.compose.state.ApplicationRowState
import org.unifiedpush.distributor.utils.appInfoForMetadata
import org.unifiedpush.distributor.utils.getApplicationIcon
import org.unifiedpush.distributor.utils.getApplicationName

fun Context.applicationRowState(packageName: String, description: String? = null): ApplicationRowState {
    val ai = appInfoForMetadata(packageName)
    val title = ai?.let { getApplicationName(it) } ?: packageName
    val icon = getApplicationIcon(packageName)
    val description = if (title == packageName) {
        description ?: ""
    } else {
        description?.let { "$it — $packageName" }
            ?: packageName
    }
    return ApplicationRowState(
        icon = icon,
        title = title,
        packageName = packageName,
        description = description
    )
}
