package org.unifiedpush.distributor.sunup.activities

import android.content.Context
import org.unifiedpush.android.distributor.ui.compose.state.RegistrationListState
import org.unifiedpush.android.distributor.ui.compose.state.RegistrationState
import org.unifiedpush.distributor.Database
import org.unifiedpush.distributor.sunup.DatabaseFactory

fun getRegistrationListState(context: Context): RegistrationListState {
    return RegistrationListState(
        list = DatabaseFactory.getDb(context).listApps().map { app ->
            getRegistrationState(context, app)
        }
    )
}

fun getRegistrationState(context: Context, app: Database.App): RegistrationState {
    return RegistrationState(
        app = context.applicationRowState(app.packageName, app.description),
        msgCount = app.msgCount,
        token = app.connectorToken,
        copyable = false
    )
}
