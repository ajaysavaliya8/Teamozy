package com.example.teamozy.core.network

import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun friendlyNetError(e: Throwable): String = when (e) {
    is UnknownHostException -> "Can’t reach server. Check your internet or server URL."
    is SocketTimeoutException -> "Server timed out. Please try again."
    else -> e.message ?: "Network error, please try again."
}
