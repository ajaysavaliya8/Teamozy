package com.example.teamozy.core.image

import android.content.Context
import coil.ImageLoader
import coil.util.DebugLogger
import com.example.teamozy.core.network.NetworkModule

/**
 * Singleton ImageLoader for Coil that uses the authenticated OkHttpClient
 * This ensures all image loading requests include the Bearer token
 */
object CoilImageLoader {

    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: createImageLoader(context).also { instance = it }
        }
    }

    private fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(NetworkModule.okHttp) // ✅ Use the authenticated OkHttpClient
            .logger(DebugLogger()) // Enable logging for debugging
            .crossfade(true) // Smooth image transitions
            .respectCacheHeaders(false) // Force reload to get fresh images
            .build()
    }
}