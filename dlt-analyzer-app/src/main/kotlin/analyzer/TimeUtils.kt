package analyzer

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val DefaultDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS").withZone(ZoneId.of("UTC"))

fun Instant.formatDefault() =
    DefaultDateTimeFormatter.format(this)
