package analyzer.db

import dltcore.MessageTypeInfo
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.sql.ResultSet
import java.time.Instant

object DltLogTable : Table() {
    val id: Column<Long> = long("id").autoIncrement()
    val timestamp: Column<Instant> = timestamp("timestamp")
    val ecuId: Column<String> = varchar("ecu_id", 4).index()
    val sessionId: Column<Int> = integer("session_id")
    val timestampHeader: Column<UInt> = uinteger("timestamp_header")
    val appId: Column<String> = varchar("app_id", 4).index()
    val contextId: Column<String> = varchar("context_id", 4).index()
    val messageType: Column<MessageTypeInfo> = enumerationByName("message_type", 20)
    val message: Column<String> = text("message")
    override val primaryKey = PrimaryKey(id)
}

data class Msg(
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
    Msg(
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

fun ResultRow.toDto() =
    Msg(
        id = this[DltLogTable.id],
        timestamp = this[DltLogTable.timestamp],
        ecuId = this[DltLogTable.ecuId],
        sessionId = this[DltLogTable.sessionId],
        timestampHeader = this[DltLogTable.timestampHeader],
        appId = this[DltLogTable.appId],
        contextId = this[DltLogTable.contextId],
        messageType = this[DltLogTable.messageType],
        message = this[DltLogTable.message],
    )
