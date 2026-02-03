package app.lonecloud.prism.utils

import app.lonecloud.prism.api.ServerConnection
import app.lonecloud.prism.services.FgService
import app.lonecloud.prism.services.SourceManager
import java.text.SimpleDateFormat

fun getDebugInfo(): String {
    val date = ServerConnection.lastEventDate?.let {
        SimpleDateFormat.getDateTimeInstance().format(it.time)
    } ?: "None"
    return "ServiceStarted: ${FgService.isServiceStarted()}\n" +
        "Last Event: $date\n" +
        SourceManager.getDebugInfo()
}
