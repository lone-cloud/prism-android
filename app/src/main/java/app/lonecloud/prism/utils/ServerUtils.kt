package app.lonecloud.prism.utils

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun normalizeUrl(rawUrl: String): String = rawUrl.trim().let {
    if (!it.startsWith("http://") && !it.startsWith("https://")) {
        "https://$it"
    } else {
        it
    }
}.trimEnd('/')

fun isValidUrl(rawUrl: String): Boolean = normalizeUrl(rawUrl).toHttpUrlOrNull() != null
