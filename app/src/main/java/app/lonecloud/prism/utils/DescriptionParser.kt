package app.lonecloud.prism.utils

object DescriptionParser {
    private const val DELIMITER = "|"

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

    fun isManualApp(description: String?): Boolean = description?.startsWith("target:") == true
}
