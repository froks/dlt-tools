package dltfilterapp

import dltcore.asIntValue
import java.io.File

internal fun String.verifyAndFormatAppIds(): String? {
    val appIds = this.split(',', ';', ' ').map { it.trim().uppercase() }.filter { it.isNotBlank() }
    if (appIds.isEmpty()) {
        return null
    }
    for (appId in appIds) {
        appId.asIntValue()
    }
    return appIds.joinToString(";")
}

internal fun String.toExistingDirectory(): File? {
    val f = File(this)
    if (f.exists() && f.isDirectory) {
        return f
    }
    return null;
}
