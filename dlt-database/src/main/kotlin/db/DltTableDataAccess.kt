package db

import dltcore.DltMessageV1
import dltcore.MessageTypeInfo
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.*
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.microseconds

private val CreateDltLogTable = """
CREATE TABLE IF NOT EXISTS DLT_LOG (
    id BIGINT PRIMARY KEY,
    ts_sec BIGINT NOT NULL,
    ts_nano BIGINT NOT NULL,
    ecu_id VARCHAR(4) NOT NULL,
    session_id INTEGER NOT NULL,
    timestamp_header INTEGER NOT NULL,
    app_id VARCHAR(4) NOT NULL,
    context_id VARCHAR(4) NOT NULL,
    message_type VARCHAR(30) NOT NULL,
    message TEXT NOT NULL
)
""".trimIndent()


class DltTableDataAccess(private val dataSource: DataSource) {
    private val logger = LoggerFactory.getLogger(DltTableDataAccess::class.java)
    private val database = Database.connect(dataSource)

    fun initialize() {
        val duration = measureTimeMillis {
            database.useConnection { connection ->
                connection.createStatement().use { it.executeUpdate("DROP TABLE IF EXISTS DLT_LOG") }
                connection.createStatement().use { it.executeUpdate(CreateDltLogTable) }
                connection.commit()
            }
        }
        logger.debug("Initializing database table took $duration ms")
    }

    fun createInserter(): DltInserter {
        val connection = dataSource.connection
        val stmt =
            connection.prepareStatement("INSERT INTO DLT_LOG (id, ts_sec, ts_nano, ecu_id, session_id, timestamp_header, app_id, context_id, message_type, message) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        return DltInserter(connection, stmt)
    }


    fun createIndexes() {
        val duration = measureTimeMillis {
            database.useConnection { connection ->
                connection.createStatement().use {
                    it.executeUpdate("CREATE INDEX IF NOT EXISTS DL_APPID ON DLT_LOG (app_id)")
                    it.executeUpdate("CREATE INDEX IF NOT EXISTS DL_ECUID ON DLT_LOG (ecu_id)")
                    it.executeUpdate("CREATE INDEX IF NOT EXISTS DL_CTXID ON DLT_LOG (context_id)")
                    it.executeUpdate("CREATE INDEX IF NOT EXISTS DL_TS ON DLT_LOG (ts_sec)")
//                it.executeUpdate("CREATE INDEX IF NOT EXISTS DL_MESSAGE ON DLT_LOG (LOWER(message))")
                    connection.commit()
                }
            }
        }
        logger.debug("Creating indexes took $duration ms")
    }

    fun readData(
        sqlClausesOr: List<ColumnDeclaring<Boolean>>,
        offset: Int? = null,
        limit: Int? = null
    ): List<DltMessageDto> {

/*
        database
            .from(DltLog)
            .select()
            .whereWithOrConditions { list -> list.addAll(sqlClausesOr) }
            .orderBy(DltLog.id.asc())
            .limit(offset, limit)
            .mapTo(list) { row -> DltLog.createEntity(row) }
*/

        val sql = database
            .from(DltLog)
            .select(
                DltLog.id,
                DltLog.timestampSeconds, DltLog.timestampNanos,
                DltLog.ecuId,
                DltLog.appId,
                DltLog.contextId,
                DltLog.timestampHeader,
                DltLog.sessionId,
                DltLog.messageType,
                DltLog.message,
            )
            .whereWithOrConditions { where -> where.addAll(sqlClausesOr) }
            .orderBy(DltLog.id.asc())
            .limit(offset, limit)

        val list = mutableListOf<DltMessageDto>()

        database.useConnection { connection ->
            val expr = database.formatExpression(sql.expression)
            connection.prepareStatement(expr.first).use { stmt ->
                val params = expr.second
                params.forEachIndexed { index, param ->
                    when (param.sqlType) {
                        VarcharSqlType -> stmt.setString(index + 1, param.value as String?)
                        IntSqlType -> stmt.setInt(index + 1, param.value as Int)
                        LongSqlType -> stmt.setLong(index + 1, param.value as Long)
                    }
                }
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            DltMessageDto(
                                id = rs.getLong(1),
                                timestamp = Instant.ofEpochSecond(rs.getLong(2), rs.getLong(3)),
                                ecuId = rs.getString(4) ?: "",
                                appId = rs.getString(5) ?: "",
                                contextId = rs.getString(6) ?: "",
                                timestampHeader = rs.getInt(7).toUInt() ?: 0u,
                                sessionId = rs.getInt(8),
                                messageType = MessageTypeInfo.valueOf(rs.getString(9)!!),
                                message = rs.getString(10) ?: "",
                            )
                        )
                    }
                }
            }

        }

        return list
    }

    fun getEntryCount(sqlClausesOr: List<ColumnDeclaring<Boolean>>): Long {
        try {
            val sql = database
                .from(DltLog)
                .select(count(DltLog.id))
                .whereWithOrConditions { list -> list.addAll(sqlClausesOr) }

            return database.executeQuery(sql.expression).use {
                it.first()
                it.getLong(1)
            }
        } catch (e: Exception) {
            logger.error("Error while retrieving count", e)
            return -1
        }
    }
}

class DltInserter(private val connection: Connection, private val stmt: PreparedStatement) : AutoCloseable {
    private val idGenerator = AtomicInteger(0)

    fun insertMsg(m1: DltMessageV1) {
        stmt.setInt(1, idGenerator.getAndIncrement())
        stmt.setLong(2, m1.storageHeader.timestampEpochSeconds)
        stmt.setLong(3, m1.storageHeader.timestampMicroseconds.microseconds.inWholeNanoseconds)
        stmt.setString(4, m1.storageHeader.ecuIdText)
        stmt.setInt(5, m1.standardHeader.sessionId ?: 0)
        stmt.setLong(6, m1.standardHeader.timestamp?.toLong() ?: 0)
        stmt.setString(7, m1.extendedHeader?.apIdText ?: "")
        stmt.setString(8, m1.extendedHeader?.ctIdText ?: "")
        stmt.setString(9, m1.extendedHeader?.messageTypeInfo?.name ?: "UNKNOWN")
        stmt.setString(10, m1.payload.logMessage)
        stmt.addBatch()
    }

    fun executeBatch() =
        stmt.executeBatch()

    fun commit() =
        connection.commit()

    val index: Int
        get() = idGenerator.get()


    override fun close() {
        stmt.close()
        connection.close()
    }
}
