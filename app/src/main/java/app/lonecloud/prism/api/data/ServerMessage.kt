package app.lonecloud.prism.api.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("messageType")
sealed class ServerMessage {
    /**
     * pub enum ServerMessage {
     *     Hello {
     *         uaid: String,
     *         status: u32,
     *         // This is required for output, but will always be "true"
     *         use_webpush: bool,
     *         broadcasts: HashMap<String, BroadcastValue>,
     *     },
     *
     * Note: we ignore broadcasts
     */
    @Serializable
    @SerialName("hello")
    @Suppress("ConstructorParameterNaming")
    data class Hello(
        val uaid: String?,
        val status: UInt,
        val use_webpush: Boolean
    ) : ServerMessage()

    /**
     *     Register {
     *         #[serde(rename = "channelID")]
     *         channel_id: Uuid,
     *         status: u32,
     *         #[serde(rename = "pushEndpoint")]
     *         push_endpoint: String,
     *     },
     */
    @Serializable
    @SerialName("register")
    data class Register(
        val channelID: String,
        val status: UInt,
        val pushEndpoint: String
    ) : ServerMessage()

    /**
     *     Unregister {
     *         #[serde(rename = "channelID")]
     *         channel_id: Uuid,
     *         status: u32,
     *     },
     */
    @Serializable
    @SerialName("unregister")
    data class Unregister(val channelID: String, val status: UInt) : ServerMessage()

    /**
     *     Broadcast {
     *         broadcasts: HashMap<String, BroadcastValue>,
     *     },
     *
     * Note: we just ignore them
     */
    @Serializable
    @SerialName("broadcast")
    data object Broadcast : ServerMessage()

    /**
     *     pub struct Notification {
     *     #[serde(rename = "channelID")]
     *     pub channel_id: Uuid,
     *     pub version: String,
     *     #[serde(default = "default_ttl", skip_serializing)]
     *     pub ttl: u64,
     *     #[serde(skip_serializing)]
     *     pub topic: Option<String>,
     *     #[serde(skip_serializing)]
     *     pub timestamp: u64,
     *     #[serde(skip_serializing_if = "Option::is_none")]
     *     pub data: Option<String>,
     *     #[serde(skip_serializing)]
     *     pub sortkey_timestamp: Option<u64>,
     *     #[serde(skip_serializing_if = "Option::is_none")]
     *     pub headers: Option<HashMap<String, String>>,
     *     #[serde(skip_serializing_if = "Option::is_none")]
     *     pub reliability_id: Option<String>,
     * }
     *
     */
    @Serializable
    @SerialName("notification")
    data class Notification(
        val channelID: String,
        val version: String,
        val data: String?,
        val headers: HashMap<String, String>?
    ) : ServerMessage()

    /**
     *    Urgency {
     *         status: u32,
     *     },
     */
    @Serializable
    @SerialName("urgency")
    data class Urgency(val status: UInt) : ServerMessage()

    /**
     *     Ping,
     */
    @Serializable
    @SerialName("ping")
    data object Ping : ServerMessage()

    companion object {
        @Suppress("SwallowedException")
        fun deserialize(jsonStr: String): ServerMessage? {
            val json = Json { ignoreUnknownKeys = true }
            return try {
                json.decodeFromString<ServerMessage>(jsonStr)
            } catch (e: SerializationException) {
                try {
                    if (json.decodeFromString<Map<String, String>>(jsonStr).isEmpty()) {
                        Ping
                    } else {
                        null
                    }
                } catch (innerE: SerializationException) {
                    android.util.Log.w("ServerMessage", "Failed to deserialize: $jsonStr", innerE)
                    null
                }
            }
        }
    }
}
