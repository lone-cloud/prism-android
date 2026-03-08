package app.lonecloud.prism.utils

import java.net.URI

fun redactIdentifier(
    value: String?,
    visiblePrefix: Int = 4,
    visibleSuffix: Int = 3
): String {
    if (value.isNullOrBlank()) return "<none>"
    val trimmed = value.trim()
    if (trimmed.length <= visiblePrefix + visibleSuffix + 1) return "***"
    val prefix = trimmed.take(visiblePrefix)
    val suffix = trimmed.takeLast(visibleSuffix)
    return "$prefix***$suffix"
}

fun redactUrl(value: String?): String {
    if (value.isNullOrBlank()) return "<none>"
    return try {
        val uri = URI(value.trim())
        val scheme = uri.scheme ?: return "<invalid-url>"
        val host = uri.host ?: return "$scheme://<redacted>"
        val port = if (uri.port != -1) ":${uri.port}" else ""
        "$scheme://$host$port"
    } catch (_: IllegalArgumentException) {
        "<invalid-url>"
    }
}
