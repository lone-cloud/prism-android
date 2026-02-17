package app.lonecloud.prism.api.data

import org.json.JSONArray
import org.json.JSONObject

data class NotificationPayload(
    val title: String,
    val message: String,
    val tag: String,
    val actions: List<NotificationAction>
) {
    companion object {
        fun fromJson(jsonString: String): NotificationPayload? = try {
            val json = JSONObject(jsonString)
            val title = json.optString("Title", "")
            val message = json.optString("Message", "")
            val tag = json.optString("Tag", "")

            val actionsArray = json.optJSONArray("Actions") ?: JSONArray()
            val actions = mutableListOf<NotificationAction>()

            for (i in 0 until actionsArray.length()) {
                val actionObj = actionsArray.getJSONObject(i)
                val action = NotificationAction(
                    id = actionObj.optString("ID", ""),
                    label = actionObj.optString("Label", ""),
                    endpoint = actionObj.optString("Endpoint", ""),
                    method = actionObj.optString("Method", "POST"),
                    data = parseDataMap(actionObj.optJSONObject("Data"))
                )
                actions.add(action)
            }

            NotificationPayload(title, message, tag, actions)
        } catch (e: Exception) {
            null
        }

        private fun parseDataMap(dataObj: JSONObject?): Map<String, Any> {
            if (dataObj == null) return emptyMap()

            val map = mutableMapOf<String, Any>()
            val keys = dataObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = dataObj.get(key)
                map[key] = value
            }
            return map
        }
    }
}

data class NotificationAction(
    val id: String,
    val label: String,
    val endpoint: String,
    val method: String,
    val data: Map<String, Any>
)
