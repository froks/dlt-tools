package db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dltcore.DltMessageParser
import dltcore.DltMessageV1
import dltcore.DltReadProgress
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import kotlin.concurrent.thread
import kotlin.math.roundToInt

object DltManager {
    private val logger = LoggerFactory.getLogger(DltManager::class.java)

    private fun isExistingFileComplete(dbFile: File, dltFile: File): Boolean {
        DriverManager.getConnection("jdbc:sqlite:file:${dbFile.absolutePath}").use { connection ->
            val expected = DltTableDataAccess.getEntryCount(connection)
            var count: Long = 0
            DltMessageParser.parseFileWithCallback(dltFile.toPath()) { _, _ ->
                count++
            }
            logger.info("Existing database '${dbFile.name}' has $count entries, expected $expected")
            return expected == count
        }

    }

    fun openFile(
        dltFile: File,
        waitForCompleteDatabase: Boolean = true,
        force: Boolean = false,
        onFinished: () -> Unit = {},
        callback: (DltReadProgress) -> Unit = { }
    ): DltTarget {
        logger.info("Opening ${dltFile.absolutePath}")

        val name = dltFile.nameWithoutExtension
        val dbFile = File("${dltFile.absolutePath}.db")
        if (dbFile.exists()) {
            if (force) {
                logger.info("Deleting existing database file ${dbFile.absolutePath}")
                dbFile.delete()
            }
        }

        val reuseExisting = dbFile.exists() && isExistingFileComplete(dbFile, dltFile)

        if (reuseExisting) {
            logger.info("Reusing existing database file ${dbFile.absolutePath}")
        }

        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:file:${dbFile.absolutePath}"
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_READ_UNCOMMITTED"
        val dataSource = HikariDataSource(config)

        if (!reuseExisting) {
            dataSource.connection.use { connection ->
                val dataAccess = DltTableDataAccess(dataSource)
                dataAccess.initialize()
                connection.commit()
            }
        }

        val dltTarget = DltTarget(UUID.randomUUID(), name, dltFile, dataSource, reuseExisting)

        if (reuseExisting) {
            dltTarget.isLoaded = true
            onFinished()
            return dltTarget
        }

        val t = thread(isDaemon = true) {
            var lastPercent = 0
            val id = AtomicInteger(1)
            val dataAccess = DltTableDataAccess(dataSource)

            dataAccess.createInserter().use { inserter ->
                DltMessageParser.parseFileWithCallback(dltFile.toPath()) { msg, progress ->
                    inserter.insertMsg(msg as DltMessageV1)
                    if (id.get() % 20_000 == 0) {
                        inserter.executeBatch()
                        inserter.commit()
                    }

                    val percent = ((progress.progress ?: 0.0f) * 100).roundToInt()
                    if (percent > lastPercent) {
                        callback.invoke(progress)
                        lastPercent = percent
                    }

                }
                inserter.executeBatch()
                inserter.commit()
            }

            dltTarget.isLoaded = true
            onFinished()

            dataAccess.createIndexes()
        }

        if (waitForCompleteDatabase) {
            t.join()
        }
        return dltTarget
    }
}

data class DltTarget(
    val id: UUID,
    val name: String,
    val dltFile: File?,
    val dataSource: DataSource,
    var isLoaded: Boolean = false,
)