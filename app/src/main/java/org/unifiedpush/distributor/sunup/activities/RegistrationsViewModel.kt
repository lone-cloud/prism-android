package org.unifiedpush.distributor.sunup.activities

import android.content.Context
import org.unifiedpush.android.distributor.ui.compose.state.RegistrationListState
import org.unifiedpush.android.distributor.ui.compose.state.RegistrationState
import org.unifiedpush.distributor.Database
import org.unifiedpush.distributor.sunup.DatabaseFactory
import org.unifiedpush.distributor.utils.appInfoForMetadata
import org.unifiedpush.distributor.utils.getApplicationIcon
import org.unifiedpush.distributor.utils.getApplicationName

fun getRegistrationListState(context: Context): RegistrationListState {
    return RegistrationListState(
        list = emptyList<RegistrationState?>()
            .toMutableList().also { appList ->
                DatabaseFactory.getDb(context).let { db ->
                    db.listTokens().forEach {
                        appList.add(
                            getRegistrationState(context, db, it)
                        )
                    }
                }
            }.filterNotNull()
    )
}

fun getRegistrationState(context: Context, db: Database, token: String): RegistrationState? {
    val app = db.getAppFromCoToken(token) ?: return null
    val ai = context.appInfoForMetadata(app.packageName)
    val title = ai?.let { context.getApplicationName(it) } ?: app.packageName
    val icon = context.getApplicationIcon(app.packageName)
    val description = if (title == app.packageName) {
        ""
    } else {
        app.packageName
    }
    return RegistrationState(
        icon = icon,
        title = title,
        description = description,
        msgCount = app.msgCount,
        token = token,
        copyable = false
    )
}
