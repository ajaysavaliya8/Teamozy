package com.hrms.jeejateamozy.core.utils

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object PrivateFileUrl {
    fun build(apiBase: String, rawPath: String?): String? {
        if (rawPath.isNullOrBlank()) return null
        if (rawPath.startsWith("http://") || rawPath.startsWith("https://")) return rawPath
        val cleaned = rawPath.trimStart('/')
        return "${apiBase.trimEnd('/')}/file".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("path", cleaned)
            ?.build()
            ?.toString()
    }
}
