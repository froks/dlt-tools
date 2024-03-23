package analyzer

import analyzer.db.DltLogTable
import analyzer.db.Msg
import analyzer.db.toDto
import dltcore.DltMessage
import dltcore.DltMessageParser
import dltcore.DltMessageV1
import dltcore.MessageTypeInfo
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    File("test.db").delete()
    val db = Database.connect("jdbc:h2:file:./test.db", driver = "org.h2.Driver")
//    val db = Database.connect("jdbc:sqlite:./test.db")

    transaction(db) {
        val createTable = DltLogTable.createStatement()
        println(createTable)
        execInBatch(createTable)

        lateinit var messages: List<DltMessage>
        val durationRead = measureTimeMillis {
            messages = DltMessageParser.parseFileAsObjects(File(args[0]).toPath())
        }
        println("read $durationRead ms")

        val id = AtomicInteger()
        val durationInsert = measureTimeMillis {
            // takes ~18-20s on h2 and sqlite for ~1.6GB dlt logs
            DltLogTable.batchInsert(messages, ignore = false, shouldReturnGeneratedValues = false) { msg ->
                val dltMsg = msg as DltMessageV1
                id.getAndIncrement()
//                this[DltLogTable.id] = id.getAndIncrement()
                this[DltLogTable.timestamp] = dltMsg.storageHeader.utcTimestamp
                this[DltLogTable.ecuId] = dltMsg.storageHeader.ecuIdText
                this[DltLogTable.sessionId] = dltMsg.standardHeader.sessionId ?: 0
                this[DltLogTable.timestampHeader] = dltMsg.standardHeader.timestamp ?: 0.toUInt()
                this[DltLogTable.appId] = dltMsg.extendedHeader?.apIdText ?: ""
                this[DltLogTable.contextId] = dltMsg.extendedHeader?.ctIdText ?: ""
                this[DltLogTable.messageType] = dltMsg.messageTypeInfo ?: MessageTypeInfo.UNKNOWN
                this[DltLogTable.message] = dltMsg.payload.logMessage
                if (id.get() % 20_000 == 0) {
                    println("progress: ${id.get()} / ${messages.size}")
                    commit()
                }
            }
            commit()
        }
        println("insert $durationInsert ms")

        lateinit var data: List<Msg>
        val duration = measureTimeMillis {
            data = DltLogTable.selectAll()
                .orderBy(DltLogTable.id)
                .limit(1000, 1000)
                .map { it.toDto() }
        }
        println("reading 1000 entries at offset 1000 took $duration ms")
        val appIds = args[1].split(",")
        val duration2 = measureTimeMillis {
            data = DltLogTable.selectAll()
                .where { DltLogTable.appId.inList(appIds) }
                .orderBy(DltLogTable.id)
                .limit(1000, 0)
                .map { it.toDto() }
        }
        println("reading first 1000 entries for ${appIds.size} appids took $duration2 ms")
//        println(data.joinToString("\n") { it.message.trimEnd() })
    }
}

