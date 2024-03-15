import lib.DltMessageParser
import lib.DltMessageV1
import lib.MessageType
import lib.asIntValue
import java.net.URI
import java.nio.file.Path
import java.text.NumberFormat
import java.time.Duration
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Must provide file and appIds")
        exitProcess(1)
    }
    val path = Path.of(URI.create("file://${args[0]}"))
    val start = System.nanoTime()
    val appIds = args[1].split("[;,]").toSet()
    val appIdsInts = appIds.map { it.asIntValue() }
    var counter = 0
    DltMessageParser().parseFileWithCallback(path) { msg, _ ->
        counter++
        if (msg is DltMessageV1) {
            if (!appIdsInts.contains(msg.extendedHeader?.apid)) {
                return@parseFileWithCallback
            }
            if (msg.extendedHeader?.messageType != MessageType.DLT_TYPE_LOG) {
                return@parseFileWithCallback
            }
            val payload = msg.payload.logMessage.removeSuffix("\n")
            println("${msg.messageTypeInfo} ${msg.storageHeader.utcTimestamp} ${msg.extendedHeader!!.apIdText} ${msg.extendedHeader!!.ctIdText} $payload")
        } else {
            throw UnsupportedOperationException("non v1 message found")
        }
    }
//    val messages = DltMessageParser().parseFileAsObjects(path)
//    println(messages.size)
    val duration = Duration.ofNanos(System.nanoTime() - start)
    val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000 / 1000
    println("$usedMemory MiB")
    println("duration: ${duration.toMillis()} ms for processing ${NumberFormat.getInstance().format(counter)} messages")
}
























