package app.lonecloud.prism.utils

object DescriptionParser {
    private const val DELIMITER = "|"

    const val VAPID_PRIVATE_KEY_PREFIX = "vp:"
    const val NAME_PREFIX = "name:"

    fun extractValue(description: String?, prefix: String): String? = description
        ?.split(DELIMITER)
        ?.firstOrNull { it.startsWith(prefix) }
        ?.substringAfter(prefix)
        ?.takeIf { it.isNotBlank() }

    fun updateValue(
        description: String?,
        prefix: String,
        value: String
    ): String {
        val parts = description
            ?.split(DELIMITER)
            ?.filter { !it.startsWith(prefix) }
            ?.toMutableList()
            ?: mutableListOf()
        parts.add("$prefix$value")
        return parts.joinToString(DELIMITER)
    }

    fun removeValue(description: String?, prefix: String): String = description
        ?.split(DELIMITER)
        ?.filter { !it.startsWith(prefix) }
        ?.joinToString(DELIMITER)
        ?: (description ?: "")

    fun isManualApp(description: String?): Boolean = description?.startsWith("target:") == true
}
