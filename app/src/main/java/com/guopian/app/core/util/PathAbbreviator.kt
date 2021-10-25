package com.guopian.app.core.util

object PathAbbreviator {
    fun abbreviate(path: String, maxChars: Int = 40): String {
        if (path.length <= maxChars) return path
        val parts = path.split('/').filter { it.isNotEmpty() }
        if (parts.size <= 2) {
            return path.take(maxChars / 2) + "..." + path.takeLast(maxChars / 2 - 1)
        }
        val head = parts.first()
        val tail = parts.takeLast(2).joinToString("/")
        val candidate = "$head/.../$tail"
        return if (candidate.length <= maxChars) {
            candidate
        } else {
            path.take(12) + "..." + path.takeLast(maxChars - 13)
        }
    }

    /** 只显示父目录，避免与文件名重复。根目录或不含路径时返回 null。 */
    fun parentDirectory(relativePath: String, fileName: String): String? {
        val normalized = relativePath.trim().trim('/')
        if (normalized.isEmpty()) return null
        val withoutName = when {
            normalized.equals(fileName, ignoreCase = true) -> ""
            normalized.endsWith("/$fileName", ignoreCase = true) ->
                normalized.dropLast(fileName.length + 1)
            else -> normalized.substringBeforeLast('/', missingDelimiterValue = "")
        }.trim('/')
        if (withoutName.isEmpty()) return null
        return abbreviate(withoutName, maxChars = 36)
    }
}
