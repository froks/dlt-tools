package db

import dltcore.DltMessageV1
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
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

    fun initialize() {
        val duration = measureTimeMillis {
            dataSource.connection.use { connection ->
                connection.createStatement().use { it.executeUpdate("DROP TABLE IF EXISTS DLT_LOG") }
                connection.createStatement().use { it.executeUpdate(CreateDltLogTable) }
                connection.commit()
            }
        }
        logger.debug("Initializing database table took $duration ms")
    }

    fun createInserter(): DltInserter {
        val connection = dataSource.connection
        val stmt = connection.prepareStatement("INSERT INTO DLT_LOG (id, ts_sec, ts_nano, ecu_id, session_id, timestamp_header, app_id, context_id, message_type, message) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        return DltInserter(connection, stmt)
    }


    fun createIndexes() {
        val duration = measureTimeMillis {
            dataSource.connection.use { connection ->
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

    fun readDataPrepared(sqlClause: String, fillStatement: (PreparedStatement) -> Unit): List<DltMessageDto> {
        val list = mutableListOf<DltMessageDto>()
        val duration = measureTimeMillis {
            dataSource.connection.use { connection ->
                connection.prepareStatement("""
                        SELECT id,ts_sec,ts_nano,ecu_id,session_id,timestamp_header,app_id,context_id,message_type,message 
                        FROM DLT_LOG 
                        WHERE $sqlClause
                    """.trimIndent())
                    .use { stmt ->
                        fillStatement.invoke(stmt)
                        val rs = stmt.executeQuery()
                        while (rs.next()) {
                            list.add(rs.toDto())
                        }
                    }
            }
        }
        logger.debug("Reading data for '$sqlClause' took $duration ms")
        return list
    }

    fun readData(sqlClause: String): List<DltMessageDto> {
        val list = mutableListOf<DltMessageDto>()
        val duration = measureTimeMillis {
            dataSource.connection.use { connection ->
                connection.prepareStatement("""
                        SELECT id,ts_sec,ts_nano,ecu_id,session_id,timestamp_header,app_id,context_id,message_type,message 
                        FROM DLT_LOG 
                        WHERE $sqlClause
                    """.trimIndent())
                    .use { stmt ->
                        val rs = stmt.executeQuery()
                        while (rs.next()) {
                            list.add(rs.toDto())
                        }
                    }
            }
        }
        logger.debug("Reading data for '$sqlClause' took $duration ms")
        return list
    }

    fun getEntryCount(): Long {
        try {
            dataSource.connection.use { connection ->
                return Companion.getEntryCount(connection)
            }
        } catch (e: Exception) {
            return -1
        }
    }

    companion object {
        fun getEntryCount(connection: Connection): Long {
            connection.prepareStatement("SELECT COUNT(id) FROM DLT_LOG").use { stmt ->
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return rs.getLong(1)
                    } else {
                        return -1
                    }
                }
            }
        }
    }
}


class DltInserter(private val connection: Connection, private val stmt: PreparedStatement) : AutoCloseable {
    private val idGenerator = AtomicInteger(1)

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


    override fun close() {
        stmt.close()
        connection.close()
    }
}
