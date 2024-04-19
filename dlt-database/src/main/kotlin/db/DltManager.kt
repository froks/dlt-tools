package db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dltcore.DltMessageParser
import dltcore.DltMessageV1
import dltcore.DltReadStatus
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import javax.sql.DataSource
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

object DltManager {
    private val logger = LoggerFactory.getLogger(DltManager::class.java)

    private fun isExistingFileComplete(dbFile: File, dltFile: File, dataAccess: DltTableDataAccess): Boolean {
        val expected = dataAccess.getEntryCount(emptyList())
        val count: Long = DltMessageParser.parseFileWithCallback(dltFile.toPath()).count()
        logger.info("Existing database '${dbFile.name}' has $count entries, expected $expected")
        return expected == count

    }

    fun openFile(
        dltFile: File,
        waitForCompleteDatabase: Boolean = true,
        force: Boolean = false,
        onFinished: (DltTarget) -> Unit = {},
        callback: (DltReadStatus) -> Unit = { }
    ) {
        logger.info("Opening ${dltFile.absolutePath}")

        val name = dltFile.nameWithoutExtension
        val dbFile = File("${dltFile.absolutePath}.db")
        if (dbFile.exists()) {
            if (force) {
                logger.info("Deleting existing database file ${dbFile.absolutePath}")
                dbFile.delete()
            }
        }

        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:file:${dbFile.absolutePath}"
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_READ_UNCOMMITTED"
        val dataSource = HikariDataSource(config)
        val dataAccess = DltTableDataAccess(dataSource)

        val reuseExisting = dbFile.exists() && isExistingFileComplete(dbFile, dltFile, dataAccess)

        if (reuseExisting) {
            logger.info("Reusing existing database file ${dbFile.absolutePath}")
            dataAccess.createIndexes()
        }

        if (!reuseExisting) {
            dataAccess.initialize()
        }

        val dltTarget = DltTarget(UUID.randomUUID(), name, dltFile, dataSource, reuseExisting)

        if (reuseExisting) {
            dltTarget.isLoaded = true
            onFinished(dltTarget)
            return
        }

        val t = thread(isDaemon = true) {
            var lastPercent = 0

            logger.info("Parsing and inserting into database ${dltFile.absolutePath}")
            val insertDuration = measureTimeMillis {
                dataAccess.createInserter().use { inserter ->

                    DltMessageParser.parseFileWithCallback(dltFile.toPath()).forEach { progress ->
                        inserter.insertMsg(progress.dltMessage as DltMessageV1)
                        if (inserter.index % 20_000 == 0) {
                            inserter.executeBatch()
                            inserter.commit()
                        }

                        val percent = ((progress.progress ?: 0.0f) * 100).roundToInt()
                        if (percent > lastPercent) {
                            callback.invoke(progress.copy(progressText = "Loading file ${dltFile.absolutePath}"))
                            lastPercent = percent
                        }

                    }
                    inserter.executeBatch()
                    inserter.commit()
                }
            }

            logger.info("Adding data to database took $insertDuration ms")

            callback.invoke(DltReadStatus(-1, null, null, null, "Creating indexes", 0, 0, null))

            logger.info("Start creating indexes")
            val indexDuration = measureTimeMillis {
                dataAccess.createIndexes()
            }
            logger.info("Creating indexes took $indexDuration ms")

            dltTarget.isLoaded = true
            onFinished(dltTarget)
        }

        if (waitForCompleteDatabase) {
            t.join()
        }
    }
}

data class DltTarget(
    val id: UUID,
    val name: String,
    val dltFile: File?,
    val dataSource: DataSource,
    var isLoaded: Boolean = false,
) {
    val dataAccess by lazy { DltTableDataAccess(dataSource) }
}
