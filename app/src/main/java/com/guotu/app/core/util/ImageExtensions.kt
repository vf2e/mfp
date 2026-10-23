package com.guotu.app.core.util

object ImageExtensions {
    val WHITELIST = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif",
        "tif", "tiff", "avif", "jxl", "raw", "dng", "cr2", "nef", "arw", "orf", "rw2",
    )

    fun isImageFileName(name: String): Boolean {
        val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return ext in WHITELIST
    }

    fun extensionOf(name: String): String =
        name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
}
