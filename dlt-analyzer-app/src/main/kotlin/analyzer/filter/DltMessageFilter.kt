package analyzer.filter

import db.DltMessageDto
import dltcore.MessageTypeInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.awt.Color
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

enum class FilterAction {
    INCLUDE,
    EXCLUDE,
    MARKER,
}

@Serializable
data class DltMessageFilter(
    var active: Boolean = true,
    var filterName: String = "New Filter",
    var action: FilterAction = FilterAction.INCLUDE,
    var appIdActive: Boolean = false,
    var appId: String? = null,
    var contextIdActive: Boolean = false,
    var contextId: String? = null,
    var timestampActive: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    var timestampStart: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    var timestampEnd: Instant? = null,
    var messageTypeActive: Boolean = false,
    var messageTypeMin: MessageTypeInfo? = null,
    var messageTypeMax: MessageTypeInfo? = null,
    var searchTextActive: Boolean = false,
    var searchText: String? = null,
    var searchCaseInsensitive: Boolean = false,
    var searchTextIsRegEx: Boolean = false,

    var markerActive: Boolean = false,
    @Serializable(with = ColorAsStringSerializer::class)
    var textColor: Color? = null,

    var tags: Set<String> = emptySet(),
) {
    private fun textMatches(entry: DltMessageDto): Boolean {
        if (searchText != null) {
            if (!searchTextIsRegEx) {
                return entry.message.contains(searchText!!, ignoreCase = searchCaseInsensitive)
            } else {
                val options = if (searchCaseInsensitive) {
                    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
                } else {
                    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
                }
                return entry.message.matches(Regex(searchText!!, options))
            }
        }
        return false
    }

    fun matches(entry: DltMessageDto): Boolean {
        return (!appIdActive || entry.appId == appId)
                && (!contextIdActive || entry.contextId == contextId)
                && (!messageTypeActive || entry.messageType.isInRange(messageTypeMin, messageTypeMax) )
                && (!searchTextActive || textMatches(entry))
    }

    fun hasSqlClause(): Boolean =
        active && (action == FilterAction.INCLUDE || action == FilterAction.EXCLUDE)
                && ((appIdActive && !appId.isNullOrEmpty())
                || (contextIdActive && !contextId.isNullOrEmpty())
                || (messageTypeActive && (messageTypeMin != null || messageTypeMax != null))
                || (timestampActive && (timestampStart != null || timestampEnd != null))
                || (searchTextActive && !searchText.isNullOrBlank())
                )

    fun sqlClause(): String {

        if (!active || action == FilterAction.MARKER) {
            return ""
        }

        val sb = StringBuilder()
        if (action == FilterAction.INCLUDE) {
            if (appIdActive && !appId.isNullOrEmpty()) {
                sb.andSql("app_id = '${appId!!.sqlEscape()}'")
            }
            if (contextIdActive && !contextId.isNullOrEmpty()) {
                sb.andSql("context_id = '${contextId!!.sqlEscape()}'")
            }
            if (messageTypeActive && (messageTypeMin != null || messageTypeMax != null)) {
                val values = MessageTypeInfo.getRange(messageTypeMin, messageTypeMax).map { "'${it.name.sqlEscape()}'" }
                sb.andSql("message_type IN (${values.joinToString(",")})")
            }
            // TODO Timestamp
            if (searchTextActive && !searchText.isNullOrEmpty()) {
                if (!searchTextIsRegEx) {
                    if (searchCaseInsensitive) {
                        sb.andSql("INSTR(LOWER(message), '${searchText!!.lowercase().sqlEscape()})'")
                    } else {
                        sb.andSql("INSTR(message, '${searchText!!.sqlEscape()}')")
                    }
                } else {
                    sb.andSql("message REGEXP '${searchText!!.sqlEscape()})'")
                }
            }
        } else if (action == FilterAction.EXCLUDE) {
            if (appIdActive && !appId.isNullOrEmpty()) {
                sb.andSql("app_id != '${appId!!.sqlEscape()}'")
            }
            if (contextIdActive && !contextId.isNullOrEmpty()) {
                sb.andSql("context_id != '${contextId!!.sqlEscape()}'")
            }
            if (messageTypeActive && (messageTypeMin != null || messageTypeMax != null)) {
                val values = MessageTypeInfo.getRange(messageTypeMin, messageTypeMax).map { "'${it.name.sqlEscape()}'" }
                sb.andSql("message_type NOT IN (${values.joinToString(",")})")
            }
            // TODO Timestamp
            if (searchTextActive && !searchText.isNullOrEmpty()) {
                if (!searchTextIsRegEx) {
                    if (searchCaseInsensitive) {
                        sb.andSql("NOT INSTR(LOWER(message), '${searchText!!.lowercase().sqlEscape()}')")
                    } else {
                        sb.andSql("NOT INSTR(message, '${searchText!!.sqlEscape()}')")
                    }
                } else {
                    sb.andSql("message NOT REGEXP '${searchText!!.sqlEscape()}'")
                }
            }
        }
        return sb.toString()
    }
}

private fun MessageTypeInfo.isInRange(messageTypeMin: MessageTypeInfo?, messageTypeMax: MessageTypeInfo?): Boolean {
    return this in MessageTypeInfo.getRange(messageTypeMin, messageTypeMax)
}

private fun MessageTypeInfo.Companion.getRange(messageTypeMin: MessageTypeInfo?, messageTypeMax: MessageTypeInfo?): Set<MessageTypeInfo> {
    val minLvl = messageTypeMin?.ordinal ?: MessageTypeInfo.DLT_LOG_FATAL.ordinal
    val maxLvl = messageTypeMax?.ordinal ?: MessageTypeInfo.DLT_LOG_VERBOSE.ordinal
    val min = min(minLvl, maxLvl)
    val max = max(maxLvl, minLvl)

    return MessageTypeInfo.entries.filter { it.ordinal in min..max }.toSet()
}


object ColorAsStringSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Color {
        try {
            val str = decoder.decodeString()
            return Color(str.toUInt(16).toInt())
        } catch (e: Exception) {
            return Color.pink
        }
    }

    override fun serialize(encoder: Encoder, value: Color) {
        val str = value.rgb.toUInt().toString(16).padStart(8, '0')
        encoder.encodeString(str)
    }
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Instant {
        val v = decoder.decodeLong()
        return Instant.ofEpochMilli(v)
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilli())
    }
}

fun StringBuilder.andSql(sql: String) {
    if (this.isNotEmpty()) {
        this.append(" AND ")
    }
    this.append(sql)
}

fun String.sqlEscape() =
    this.replace("\"", "\\\"").replace("'", "\\'")


fun List<DltMessageFilter>?.sqlWhere(): String {
    if (this == null) {
        return "1=1"
    }
    val sqlClauses = this
        .filter { it.hasSqlClause() }
        .map { it.sqlClause() }
        .filter { it.isNotEmpty() }
        .joinToString(" OR ") { "($it)" }
    if (sqlClauses.isEmpty()) {
        return "1=1"
    }
    return sqlClauses
}

fun String.toMessageTypeInfo(): MessageTypeInfo? =
    MessageTypeInfo.getByShort(this)

