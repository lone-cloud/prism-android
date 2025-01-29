package org.unifiedpush.distributor.sunup.activities

import android.content.Context
import org.unifiedpush.android.distributor.ui.compose.state.RegistrationListState
import org.unifiedpush.android.distributor.ui.compose.state.RegistrationState
import org.unifiedpush.distributor.Database
import org.unifiedpush.distributor.sunup.DatabaseFactory
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
    val title = context.getApplicationName(app.packageName) ?: app.packageName
    val description = if (title == app.packageName) {
        ""
    } else {
        app.packageName
    }
    return RegistrationState(
        title = title,
        description = description,
        msgCount = app.msgCount,
        token = token,
        copyable = false
    )
}
