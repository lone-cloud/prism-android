package app.lonecloud.prism.utils

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

object HttpClientFactory {
    val shared: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    val longLived: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(1, TimeUnit.MINUTES)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
