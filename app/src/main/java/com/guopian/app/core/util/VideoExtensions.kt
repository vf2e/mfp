package com.guopian.app.core.util

object VideoExtensions {
    val WHITELIST = setOf(
        "mp4", "m4v", "mov", "mkv", "webm", "avi", "wmv", "flv",
        "ts", "m2ts", "mts", "3gp", "mpg", "mpeg", "rmvb", "rm", "vob", "asf",
        "f4v", "ogv", "divx", "xvid", "dat", "mpv", "mpe", "3g2",
    )

    fun isVideoFileName(name: String): Boolean {
        val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return ext in WHITELIST
    }

    fun extensionOf(name: String): String =
        name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
}
