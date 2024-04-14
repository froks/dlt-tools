package db

import dltcore.MessageTypeInfo
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.*
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

object DltLog : BaseTable<DltMessageDto>("DLT_LOG") {
    val id = long("id").primaryKey()
    val timestampSeconds = long("ts_sec")
    val timestampNanos = long("ts_nano")
    val ecuId = varchar("ecu_id")
    val sessionId = int("session_id")
    val timestampHeader = int("timestamp_header")
    val appId = varchar("app_id")
    val contextId = varchar("context_id")
    val messageType = enum<MessageTypeInfo>("message_type")
    val message = text("message")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): DltMessageDto {
        return DltMessageDto(
            id = row[id] ?: throw IllegalStateException("DLT_ID is not set"),
            timestamp = Instant.ofEpochSecond(
                row[timestampSeconds] ?: throw IllegalStateException("TS_SEC is not set"),
                row[timestampNanos] ?: throw IllegalStateException("TS_NANO is not set")
            ),
            ecuId = row[ecuId] ?: throw IllegalStateException("ECU_ID is not set"),
            sessionId = row[sessionId] ?: throw IllegalStateException("SESSION_ID is not set"),
            timestampHeader = row[timestampHeader]?.toUInt() ?: throw IllegalStateException("TIMESTAMP_HEADER is not set"),
            appId = row[appId] ?: throw IllegalStateException("APP_ID is not set"),
            contextId = row[contextId] ?: throw IllegalStateException("CONTEXT_ID is not set"),
            messageType = row[messageType] ?: throw IllegalStateException("MESSAGE_TYPE is not set"),
            message = row[message] ?: throw IllegalStateException("MESSAGE is not set"),
        )
    }
}
