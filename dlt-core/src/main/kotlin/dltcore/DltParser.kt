package dltcore

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.io.path.fileSize
import kotlin.math.min

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

private const val OVERLAP = 10_000_000

private class LargeFileBufferChooser(val path: Path) : AutoCloseable {
    private lateinit var currentBuffer: ByteBuffer

    private val fileSize = path.fileSize();
    private var fileChannel: FileChannel = FileChannel.open(path, StandardOpenOption.READ)
    private var absolutePosition = -1L
    private var bufferIndex = 0

    val buffer: ByteBuffer
        get() {
            if (absolutePosition == -1L) {
                absolutePosition = 0
                currentBuffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    absolutePosition,
                    min(fileSize, Integer.MAX_VALUE.toLong())
                )
                bufferIndex = 0
                return currentBuffer
            }
            val relativePosition = currentBuffer.position()
            if (relativePosition >= (Integer.MAX_VALUE - OVERLAP)) {
                absolutePosition += relativePosition.toLong()
                currentBuffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    absolutePosition,
                    min(fileSize - absolutePosition, Integer.MAX_VALUE.toLong())
                )
            }
            return currentBuffer
        }

    override fun close() {
        fileChannel.close()
    }
}

private class DltMessageIteratorPath(private val largeFile: LargeFileBufferChooser) : Iterator<DltReadStatus> {
    private var index: Long = 0
    private var successCount: Long = 0
    private var errorCount: Long = 0
    private val totalSize = largeFile.path.fileSize()

    private fun parseDltMessage(buffer: ByteBuffer, version: DltStorageVersion): DltMessage =
        when (version) {
            DltStorageVersion.V1 -> DltMessageV1.fromByteBuffer(buffer)
            DltStorageVersion.V2 -> throw UnsupportedOperationException("not supported yet")
        }

    override fun hasNext(): Boolean {
        val buffer = largeFile.buffer
        return buffer.hasRemaining()
    }

    override fun next(): DltReadStatus {
        val buffer = largeFile.buffer
        buffer.order(ByteOrder.BIG_ENDIAN)
        if (buffer.hasRemaining()) {
            val message = try {
                val magic = buffer.int
                val version = DltStorageVersion.getByMagic(magic)
                parseDltMessage(buffer, version)
            } catch (e: RuntimeException) {
                errorCount++
                throw RuntimeException(
                    "Error while parsing message at file position ${buffer.position()}: ${e.message}",
                    e
                )
            }
            successCount++
            val progress = buffer.position().toFloat() / totalSize.toFloat()
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
                throw RuntimeException(
                    "Error while parsing message at file position ${buffer.position()}: ${e.message}",
                    e
                )
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
        fun parseBuffer(buffer: ByteBuffer, totalSize: Long?): Stream<DltReadStatus> =
            StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                    DltMessageIterator(buffer, totalSize),
                    Spliterator.NONNULL or Spliterator.ORDERED
                ), false
            )

        fun parseFile(path: Path): Stream<DltReadStatus> {
            val fb = LargeFileBufferChooser(path)
            return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                    DltMessageIteratorPath(fb),
                    Spliterator.NONNULL or Spliterator.ORDERED
                ), false
            ).onClose {
                fb.close()
            }
        }
    }
}
