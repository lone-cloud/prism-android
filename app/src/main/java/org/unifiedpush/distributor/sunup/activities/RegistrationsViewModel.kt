package org.unifiedpush.distributor.sunup.activities

import android.content.Context
import org.unifiedpush.android.distributor.ui.compose.state.RegistrationListState
import org.unifiedpush.android.distributor.ui.compose.state.RegistrationState
import org.unifiedpush.distributor.Database
import org.unifiedpush.distributor.sunup.DatabaseFactory

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
   return RegistrationState(
        app = context.applicationRowState(app.packageName, app.description),
        msgCount = app.msgCount,
        token = token,
        copyable = false
    )
}
