package app.lonecloud.prism.utils

fun normalizeUrl(rawUrl: String): String = rawUrl.trim().let {
    if (!it.startsWith("http://") && !it.startsWith("https://")) {
        "https://$it"
    } else {
        it
    }
}.trimEnd('/')
