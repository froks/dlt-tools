package db

import dltcore.MessageTypeInfo
import java.sql.ResultSet
import java.time.Instant

data class DltMessageDto(
    val id: Long,
    val timestamp: Instant,
    val ecuId: String,
    val sessionId: Int,
    val timestampHeader: UInt,
    val appId: String,
    val contextId: String,
    val messageType: MessageTypeInfo,
    val message: String,
)

// id,ts_sec,ts_nano,ecu_id,session_id,timestamp_header,app_id,context_id,message_type,message
fun ResultSet.toDto() =
    DltMessageDto(
        id = this.getLong(1),
        timestamp = Instant.ofEpochSecond(this.getLong(2), this.getLong(3)),
        ecuId = this.getString(4),
        sessionId = this.getInt(5),
        timestampHeader = this.getLong(6).toUInt(),
        appId = this.getString(7),
        contextId = this.getString(8),
        messageType = MessageTypeInfo.valueOf(this.getString(9)),
        message = this.getString(10),
    )
