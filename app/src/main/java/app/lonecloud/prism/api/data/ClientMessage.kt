package app.lonecloud.prism.api.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import okhttp3.WebSocket

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("messageType")
sealed class ClientMessage {
    /**
     *     Hello {
     *         uaid: Option<String>,
     *         #[serde(rename = "channelIDs", skip_serializing_if = "Option::is_none")]
     *         _channel_ids: Option<Vec<Uuid>>,
     *         #[serde(skip_serializing_if = "Option::is_none")]
     *         broadcasts: Option<HashMap<String, String>>,
     *     },
     */
    @Serializable
    @SerialName("hello")
    class Hello(
        val uaid: String?,
        val channelIDs: Array<String>? = null,
        val broadcasts: HashMap<String, String>? = null
    ) : ClientMessage()

    /**
     *     Register {
     *         #[serde(rename = "channelID")]
     *         channel_id: String,
     *         key: Option<String>,
     *     },
     */
    @Serializable
    @SerialName("register")
    class Register(
        val channelID: String,
        /** VAPID key */
        val key: String? = null
    ) : ClientMessage()

    /**
     *     Unregister {
     *         #[serde(rename = "channelID")]
     *         channel_id: Uuid,
     *         code: Option<u32>,
     *     },
     */
    @Serializable
    @SerialName("unregister")
    class Unregister(val channelID: String, val code: UInt? = null) : ClientMessage()

    /**
     *     BroadcastSubscribe {
     *         broadcasts: HashMap<String, String>,
     *     },
     */
    @Serializable
    @SerialName("broadcastsubscribe")
    class BroadcastSubscribe(val broadcasts: HashMap<String, String>) : ClientMessage()

    /**
     *     Ack {
     *         updates: Vec<ClientAck>,
     *     },
     */
    @Serializable
    @SerialName("ack")
    class Ack(val updates: Array<ClientAck>) : ClientMessage()

    /**
     *     Nack {
     *         code: Option<i32>,
     *         version: String,
     *     },
     */
    @Serializable
    @SerialName("nack")
    class Nack(val code: UInt?, val version: String) : ClientMessage()

    @Serializable
    @SerialName("urgency")
    class MinUrgency(val min: Urgency) : ClientMessage()

    @Serializable
    enum class Urgency {
        @SerialName("very-low")
        VeryLow,

        @SerialName("low")
        Low,

        @SerialName("normal")
        Normal,

        @SerialName("high")
        High
    }

    /**
     *     Ping,
     */
    @Serializable
    @SerialName("ping")
    data object Ping : ClientMessage()

    /**
     * #[derive(Debug, Deserialize)]
     * pub struct ClientAck {
     *     // The channel_id which received messages
     *     #[serde(rename = "channelID")]
     *     pub channel_id: Uuid,
     *     // The corresponding version number for the message.
     *     pub version: String,
     * }
     */
    @Serializable
    data class ClientAck(val channelID: String, val version: String)

    fun serialize(): String {
        val json = Json { ignoreUnknownKeys = true }
        return when (this) {
            Ping -> "{}"
            else -> json.encodeToString<ClientMessage>(this)
        }
    }

    fun send(ws: WebSocket) {
        ws.send(this.serialize())
    }
}
