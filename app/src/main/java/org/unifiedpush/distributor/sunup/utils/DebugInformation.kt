package org.unifiedpush.distributor.sunup.utils

import java.text.SimpleDateFormat
import org.unifiedpush.distributor.sunup.api.ServerConnection
import org.unifiedpush.distributor.sunup.services.FailureCounter
import org.unifiedpush.distributor.sunup.services.FgService

fun getDebugInfo(): String {
    val date = ServerConnection.lastEventDate?.let {
        SimpleDateFormat.getDateTimeInstance().format(it.time)
    } ?: "None"
    return "ServiceStarted: ${FgService.isServiceStarted()}\n" +
        "Last Event: $date\n" +
        FailureCounter.getDebugInfo()
}
