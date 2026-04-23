package de.debugco.dltmcp

import dltcore.DltMessageParser
import dltcore.DltMessageV1
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant

private val logger = LoggerFactory.getLogger("de.debugco.dltmcp.DltMcpServer")

/**
 * Parsed representation of a single DLT message, held in memory after loading.
 */
data class DltEntry(
    val index: Long,
    val timestamp: Instant,
    val ecuId: String,
    val appId: String,
    val contextId: String,
    val messageType: String,
    val message: String,
)

fun main(): Unit = runBlocking {
    val messages = mutableListOf<DltEntry>()
    var loadedFile: String? = null

    val server = Server(
        serverInfo = Implementation(name = "dlt-mcp", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false)
            )
        )
    )

    // ── Tool: open_dlt_file ───────────────────────────────────────────────────

    server.addTool(
        name = "open_dlt_file",
        description = "Open and parse a DLT file into memory. Must be called before querying logs.",
        inputSchema = buildToolSchema {
            requiredString("path", "Absolute path to the .dlt file")
        }
    ) { request ->
        val path = request.arguments?.getString("path")
            ?: return@addTool errorResult("Missing required parameter: path")

        val file = File(path)
        if (!file.exists()) return@addTool errorResult("File not found: $path")
        if (!file.isFile) return@addTool errorResult("Not a file: $path")

        messages.clear()
        var errorCount = 0L

        try {
            DltMessageParser.parseFile(file.toPath()).forEach { status ->
                if (status.error != null) {
                    errorCount++
                    return@forEach
                }
                val msg = status.dltMessage as? DltMessageV1 ?: return@forEach
                messages.add(
                    DltEntry(
                        index = status.index,
                        timestamp = msg.storageHeader.utcTimestamp,
                        ecuId = msg.storageHeader.ecuIdText.orEmpty(),
                        appId = msg.extendedHeader?.apIdText.orEmpty(),
                        contextId = msg.extendedHeader?.ctIdText.orEmpty(),
                        messageType = msg.messageTypeInfo?.name ?: "UNKNOWN",
                        message = msg.payload.logMessage.orEmpty(),
                    )
                )
            }
            loadedFile = file.name
            val summary = buildString {
                append("Loaded ${messages.size} messages from '${file.name}'")
                if (errorCount > 0) append(" ($errorCount parse errors skipped)")
                if (messages.isNotEmpty()) {
                    append("\nTimestamp range: ${messages.first().timestamp} to ${messages.last().timestamp}")
                }
            }
            textResult(summary)
        } catch (e: Exception) {
            logger.error("Failed to parse DLT file: $path", e)
            errorResult("Failed to parse file: ${e.message}")
        }
    }

    // ── Tool: get_app_ids ─────────────────────────────────────────────────────

    server.addTool(
        name = "get_app_ids",
        description = "List all unique application IDs (appId) present in the loaded DLT file.",
        inputSchema = buildToolSchema {}
    ) { _ ->
        if (messages.isEmpty()) return@addTool errorResult("No DLT file loaded. Call open_dlt_file first.")
        val appIds = messages.map { it.appId }.filter { it.isNotBlank() }.toSortedSet()
        textResult(appIds.joinToString("\n"))
    }

    // ── Tool: get_context_ids ─────────────────────────────────────────────────

    server.addTool(
        name = "get_context_ids",
        description = "List all unique context IDs present in the loaded DLT file, optionally filtered by appId.",
        inputSchema = buildToolSchema {
            optionalString("appId", "Filter context IDs to this application ID")
        }
    ) { request ->
        if (messages.isEmpty()) return@addTool errorResult("No DLT file loaded. Call open_dlt_file first.")
        val filterApp = request.arguments?.getString("appId")
        val contextIds = messages
            .filter { filterApp == null || it.appId == filterApp }
            .map { it.contextId }
            .filter { it.isNotBlank() }
            .toSortedSet()
        textResult(contextIds.joinToString("\n"))
    }

    // ── Tool: get_ecu_ids ─────────────────────────────────────────────────────

    server.addTool(
        name = "get_ecu_ids",
        description = "List all unique ECU IDs present in the loaded DLT file.",
        inputSchema = buildToolSchema {}
    ) { _ ->
        if (messages.isEmpty()) return@addTool errorResult("No DLT file loaded. Call open_dlt_file first.")
        val ecuIds = messages.map { it.ecuId }.filter { it.isNotBlank() }.toSortedSet()
        textResult(ecuIds.joinToString("\n"))
    }

    // ── Tool: query_logs ──────────────────────────────────────────────────────

    server.addTool(
        name = "query_logs",
        description = """
            Query log messages from the loaded DLT file with optional filters.
            All filter parameters are optional and combined with AND logic.
            Use get_app_ids / get_context_ids / get_ecu_ids first to discover valid filter values.
            Timestamps must be ISO 8601 strings, e.g. "2024-01-15T10:30:00Z". The timestamp range
            of the loaded file is reported by open_dlt_file.
            Results are returned in chronological order.
        """.trimIndent(),
        inputSchema = buildToolSchema {
            optionalString("appId", "Filter by application ID (exact match)")
            optionalString("contextId", "Filter by context ID (exact match)")
            optionalString("ecuId", "Filter by ECU ID (exact match)")
            optionalString("messageType", "Filter by message type, e.g. DLT_LOG_FATAL, DLT_LOG_ERROR, DLT_LOG_WARN, DLT_LOG_INFO, DLT_LOG_DEBUG, DLT_LOG_VERBOSE")
            optionalString("contains", "Filter messages whose text contains this string (case-insensitive)")
            optionalString("timestampFrom", "Include only messages at or after this ISO 8601 timestamp, e.g. 2024-01-15T10:30:00Z")
            optionalString("timestampTo", "Include only messages at or before this ISO 8601 timestamp, e.g. 2024-01-15T10:35:00Z")
            optionalInt("limit", "Maximum number of messages to return (default: 200, max: 2000)")
            optionalInt("offset", "Number of matching messages to skip before returning results (default: 0)")
        }
    ) { request ->
        if (messages.isEmpty()) return@addTool errorResult("No DLT file loaded. Call open_dlt_file first.")

        val appId       = request.arguments?.getString("appId")
        val contextId   = request.arguments?.getString("contextId")
        val ecuId       = request.arguments?.getString("ecuId")
        val messageType = request.arguments?.getString("messageType")
        val contains    = request.arguments?.getString("contains")
        val limit       = (request.arguments?.getInt("limit") ?: 200).coerceIn(1, 2000)
        val offset      = (request.arguments?.getInt("offset") ?: 0).coerceAtLeast(0)

        val timestampFrom = request.arguments?.getString("timestampFrom")?.let {
            runCatching { Instant.parse(it) }.getOrElse { return@addTool errorResult("Invalid timestampFrom: '$it'. Use ISO 8601 format, e.g. 2024-01-15T10:30:00Z") }
        }
        val timestampTo = request.arguments?.getString("timestampTo")?.let {
            runCatching { Instant.parse(it) }.getOrElse { return@addTool errorResult("Invalid timestampTo: '$it'. Use ISO 8601 format, e.g. 2024-01-15T10:35:00Z") }
        }

        val filtered = messages.filter { entry ->
            (appId == null         || entry.appId == appId) &&
            (contextId == null     || entry.contextId == contextId) &&
            (ecuId == null         || entry.ecuId == ecuId) &&
            (messageType == null   || entry.messageType.equals(messageType, ignoreCase = true)) &&
            (contains == null      || entry.message.contains(contains, ignoreCase = true)) &&
            (timestampFrom == null || !entry.timestamp.isBefore(timestampFrom)) &&
            (timestampTo == null   || !entry.timestamp.isAfter(timestampTo))
        }

        val page = filtered.drop(offset).take(limit)

        if (page.isEmpty()) {
            return@addTool textResult("No messages matched the given filters.")
        }

        val text = buildString {
            append("Showing ${page.size} of ${filtered.size} matching messages")
            if (offset > 0) append(" (offset $offset)")
            append(" from '${loadedFile}':\n\n")
            page.forEach { entry ->
                appendLine("${entry.timestamp} [${entry.ecuId}] ${entry.appId}/${entry.contextId} ${entry.messageType}: ${entry.message}")
            }
        }
        textResult(text)
    }

    // ── Start STDIO transport ─────────────────────────────────────────────────

    logger.info("dlt-mcp server starting on stdio")
    val transport = StdioServerTransport(
        System.`in`.asSource().buffered(),
        System.out.asSink().buffered(),
    )
    val done = CompletableDeferred<Unit>()
    val session = server.createSession(transport)
    session.onClose { done.complete(Unit) }
    done.await()
    logger.info("dlt-mcp server shut down")
}

// ── DSL helpers ───────────────────────────────────────────────────────────────

private class ToolSchemaBuilder {
    val properties = mutableMapOf<String, JsonObject>()
    val required = mutableListOf<String>()

    fun requiredString(name: String, description: String) {
        properties[name] = buildJsonObject {
            put("type", "string")
            put("description", description)
        }
        required.add(name)
    }

    fun optionalString(name: String, description: String) {
        properties[name] = buildJsonObject {
            put("type", "string")
            put("description", description)
        }
    }

    fun optionalInt(name: String, description: String) {
        properties[name] = buildJsonObject {
            put("type", "integer")
            put("description", description)
        }
    }
}

private fun buildToolSchema(block: ToolSchemaBuilder.() -> Unit): ToolSchema {
    val builder = ToolSchemaBuilder().apply(block)
    return ToolSchema(
        properties = JsonObject(builder.properties),
        required = builder.required.ifEmpty { null },
    )
}

private fun JsonObject?.getString(key: String): String? =
    this?.get(key)?.jsonPrimitive?.contentOrNull

private fun JsonObject?.getInt(key: String): Int? =
    this?.get(key)?.jsonPrimitive?.intOrNull

private fun textResult(text: String) = CallToolResult(
    content = listOf(TextContent(text = text)),
    isError = false,
)

private fun errorResult(message: String) = CallToolResult(
    content = listOf(TextContent(text = "Error: $message")),
    isError = true,
)
