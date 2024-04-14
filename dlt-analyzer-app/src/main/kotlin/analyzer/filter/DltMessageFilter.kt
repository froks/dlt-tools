package analyzer.filter

import db.DltLog
import db.DltMessageDto
import dltcore.MessageTypeInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.ktorm.dsl.*
import org.ktorm.expression.FunctionExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.support.sqlite.instr
import org.ktorm.support.sqlite.toLowerCase
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
                && (!messageTypeActive || entry.messageType.isInRange(messageTypeMin, messageTypeMax))
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

    fun sqlClauses(): List<ColumnDeclaring<Boolean>> {
        if (!active || action == FilterAction.MARKER) {
            return emptyList()
        }

        val list = mutableListOf<ColumnDeclaring<Boolean>>()

        if (action == FilterAction.INCLUDE) {
            if (appIdActive && !appId.isNullOrEmpty()) {
                val appIds = appId!!.split(",", ";", " ")
                if (appIds.size == 1) {
                    list.add(DltLog.appId.eq(appIds.first()))
                } else {
                    list.add(DltLog.appId.inList(appIds))
                }
            }
            if (contextIdActive && !contextId.isNullOrEmpty()) {
                list.add(DltLog.contextId.eq(contextId!!))
            }
            if (messageTypeActive && (messageTypeMin != null || messageTypeMax != null)) {
                list.add(DltLog.messageType.inList(MessageTypeInfo.getRange(messageTypeMin, messageTypeMax)))
            }
            // TODO Timestamp
            if (searchTextActive && !searchText.isNullOrEmpty()) {
                if (!searchTextIsRegEx) {
                    if (searchCaseInsensitive) {
                        list.add(DltLog.message.toLowerCase().instr(searchText!!.lowercase()).gt(0))
                    } else {
                        list.add(DltLog.message.instr(searchText!!).gt(0))
                    }
                } else {
                    list.add(DltLog.message.regexp(searchText!!))
                }
            }
        } else if (action == FilterAction.EXCLUDE) {
            if (appIdActive && !appId.isNullOrEmpty()) {
                val appIds = appId!!.split(",", ";", " ")
                if (appIds.size == 1) {
                    list.add(DltLog.appId.neq(appIds.first()))
                } else {
                    list.add(DltLog.appId.notInList(appIds))
                }
            }
            if (contextIdActive && !contextId.isNullOrEmpty()) {
                list.add(DltLog.contextId.neq(contextId!!))
            }
            if (messageTypeActive && (messageTypeMin != null || messageTypeMax != null)) {
                list.add(DltLog.messageType.notInList(MessageTypeInfo.getRange(messageTypeMin, messageTypeMax)))
            }
            // TODO Timestamp
            if (searchTextActive && !searchText.isNullOrEmpty()) {
                if (!searchTextIsRegEx) {
                    if (searchCaseInsensitive) {
                        list.add(DltLog.message.toLowerCase().instr(searchText!!.lowercase()).eq(0))
                    } else {
                        list.add(DltLog.message.instr(searchText!!).eq(0))
                    }
                } else {
                    list.add(!DltLog.message.regexp(searchText!!))
                }
            }
        }
        return list
    }
}

private fun MessageTypeInfo.isInRange(messageTypeMin: MessageTypeInfo?, messageTypeMax: MessageTypeInfo?): Boolean {
    return this in MessageTypeInfo.getRange(messageTypeMin, messageTypeMax)
}

private fun MessageTypeInfo.Companion.getRange(
    messageTypeMin: MessageTypeInfo?,
    messageTypeMax: MessageTypeInfo?
): Set<MessageTypeInfo> {
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

fun List<DltMessageFilter>?.sqlWhere(): List<ColumnDeclaring<Boolean>> {
    if (this == null) {
        return emptyList()
    }

    return this
        .filter { it.hasSqlClause() }
        .mapNotNull {
            val clauses = it.sqlClauses()
            if (clauses.isEmpty()) {
                return@mapNotNull null
            } else {
                clauses.reduce { acc, columnDeclaring ->
                    acc.and(columnDeclaring)
                }
            }
        }
}

fun String.toMessageTypeInfo(): MessageTypeInfo? =
    MessageTypeInfo.getByShort(this)


/**
 * SQLite REGEXP function, translated to `regexp(left, right)`
 */
fun ColumnDeclaring<String>.regexp(right: String): FunctionExpression<Boolean> {
    return FunctionExpression(
        functionName = "regexp",
        arguments = listOf(this.asExpression(), wrapArgument(right)),
        sqlType = BooleanSqlType
    )
}
