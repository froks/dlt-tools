package analyzer

import analyzer.db.Msg
import analyzer.db.toDto
import dltcore.DltMessageParser
import dltcore.DltMessageV1
import java.io.File
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.microseconds

val createTable = """
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

fun main(args: Array<String>) {
    println("start")

    File("./test.db").delete()

    val connection = DriverManager.getConnection("jdbc:sqlite:file:./test.db")
//    val connection = DriverManager.getConnection("jdbc:sqlite:mem")
//    val connection = DriverManager.getConnection("jdbc:h2:file:./test.db")

    connection.autoCommit = false

    connection.createStatement().use { it.executeUpdate("DROP TABLE IF EXISTS DLT_LOG") }
    connection.createStatement().use { it.executeUpdate(createTable) }

    val durationInsert = measureTimeMillis {
        connection.prepareStatement("INSERT INTO DLT_LOG (id, ts_sec, ts_nano, ecu_id, session_id, timestamp_header, app_id, context_id, message_type, message) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
            .use { stmt ->
                val id = AtomicInteger()
                DltMessageParser.parseFileWithCallback(File(args[0]).toPath()) { msg, p ->
                    val m1 = msg as DltMessageV1
                    stmt.setInt(1, id.getAndIncrement())
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
                    if (id.get() % 20_000 == 0) {
                        val percent = ((p.filePosition?.toFloat() ?: 0f) / (p.fileSize?.toFloat() ?: 100f)) * 100
                        println("progress: ${percent.roundToInt()}%")
                        stmt.executeBatch()
                        connection.commit()
                    }
                }
                stmt.executeBatch()
                connection.commit()
            }
    }
    println("parse and insert took $durationInsert ms")

    val createIndexDuration = measureTimeMillis {
        connection.createStatement().use {
            it.executeUpdate("CREATE INDEX DL_APPID ON DLT_LOG (app_id)")
            it.executeUpdate("CREATE INDEX DL_ECUID ON DLT_LOG (ecu_id)")
            it.executeUpdate("CREATE INDEX DL_CTXID ON DLT_LOG (context_id)")
            it.executeUpdate("CREATE INDEX DL_TS ON DLT_LOG (ts_sec)")
        }
    }
    println("index duration $createIndexDuration ms")

    val data = mutableListOf<Msg>()
    val duration = measureTimeMillis {
        connection.prepareStatement("SELECT id,ts_sec,ts_nano,ecu_id,session_id,timestamp_header,app_id,context_id,message_type,message FROM DLT_LOG ORDER BY id LIMIT 1000 OFFSET 1000")
            .use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    data.add(rs.toDto())
                }
            }
    }
    println("reading 1000 entries at offset 1000 duration $duration ms")

    data.clear()
    val appIds = args[1].split(",").map { "'$it'" }
    val durationAppIds = measureTimeMillis {

        connection.prepareStatement("SELECT id,ts_sec,ts_nano,ecu_id,session_id,timestamp_header,app_id,context_id,message_type,message FROM DLT_LOG WHERE app_id IN (${appIds.joinToString(",")}) ORDER BY id LIMIT 10000")
            .use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    data.add(rs.toDto())
                }
            }
    }
    println("reading first 1000 entries for ${appIds.size} appids duration $durationAppIds ms")

//    println(data.joinToString("\n") { it.message.trimEnd() })

    connection.close()
}
