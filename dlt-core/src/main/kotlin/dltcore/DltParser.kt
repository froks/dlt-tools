package dltcore

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

data class DltReadStatus(
    val index: Long,
    val filePosition: Long?,
    val fileSize: Long?,
    val progress: Float?,
    val progressText: String?,
    val errorCount: Long,
    val successCount: Long,
    val dltMessage: DltMessage?,
)

private class DltMessageIterator(val buffer: ByteBuffer, val totalSize: Long?) : Iterator<DltReadStatus> {
    private var index: Long = 0
    private var successCount: Long = 0
    private var errorCount: Long = 0

    private fun parseDltMessage(buffer: ByteBuffer, version: DltStorageVersion): DltMessage =
        when (version) {
            DltStorageVersion.V1 -> DltMessageV1.fromByteBuffer(buffer)
            DltStorageVersion.V2 -> throw UnsupportedOperationException("not supported yet")
        }

    override fun hasNext(): Boolean =
        buffer.hasRemaining()

    override fun next(): DltReadStatus {
        buffer.order(ByteOrder.BIG_ENDIAN)
        if (buffer.hasRemaining()) {
            val message = try {
                val magic = buffer.int
                val version = DltStorageVersion.getByMagic(magic)
                parseDltMessage(buffer, version)
            } catch (e: RuntimeException) {
                errorCount++
                throw RuntimeException("Error while parsing message at file position ${buffer.position()}: ${e.message}", e)
            }
            successCount++
            val progress = if (totalSize != null) {
                buffer.position().toFloat() / totalSize.toFloat()
            } else {
                null
            }
            return DltReadStatus(
                index = index++,
                filePosition = buffer.position().toLong(),
                fileSize = totalSize,
                progress = progress,
                progressText = "Parsing file",
                errorCount = errorCount,
                successCount = successCount,
                dltMessage = message
            )
        }
        throw RuntimeException("No more data available, but next() was called on iterator")
    }
}

class DltMessageParser private constructor() {

    companion object {
        fun parseWithCallback(buffer: ByteBuffer, totalSize: Long?): Stream<DltReadStatus> =
            StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                DltMessageIterator(buffer, totalSize),
                Spliterator.NONNULL or Spliterator.ORDERED
            ), false)

        fun parseFileWithCallback(path: Path): Stream<DltReadStatus> {
            FileChannel.open(path).use { fileChannel ->
                val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
                return parseWithCallback(buffer, fileChannel.size())
            }
//            System.gc() // JDK-4715154
        }

        fun parseFileAsObjects(path: Path): List<DltMessage> =
            parseFileWithCallback(path).filter { it.dltMessage != null }.map { it.dltMessage!! }.toList()

    }
}

