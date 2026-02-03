package app.lonecloud.prism.utils

val Any.TAG: String
    get() {
        val tag = javaClass.simpleName
        return if (tag.length <= 23) tag else tag.take(23)
    }
