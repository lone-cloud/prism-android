package app.lonecloud.prism.utils

fun normalizeUrl(rawUrl: String): String = rawUrl.trim().let {
    if (!it.startsWith("http://") && !it.startsWith("https://")) {
        "https://$it"
    } else {
        it
    }
}.trimEnd('/')

fun testServerConnection(
    url: String,
    apiKey: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    app.lonecloud.prism.PrismServerClient.testConnection(
        url,
        apiKey,
        onSuccess,
        onError
    )
}
