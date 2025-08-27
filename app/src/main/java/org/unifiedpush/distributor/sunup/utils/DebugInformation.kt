package org.unifiedpush.distributor.sunup.utils

import java.text.SimpleDateFormat
import org.unifiedpush.distributor.sunup.api.ServerConnection
import org.unifiedpush.distributor.sunup.services.FgService
import org.unifiedpush.distributor.sunup.services.SourceManager

fun getDebugInfo(): String {
    val date = ServerConnection.lastEventDate?.let {
        SimpleDateFormat.getDateTimeInstance().format(it.time)
    } ?: "None"
    return "ServiceStarted: ${FgService.isServiceStarted()}\n" +
        "Last Event: $date\n" +
        SourceManager.getDebugInfo()
}
