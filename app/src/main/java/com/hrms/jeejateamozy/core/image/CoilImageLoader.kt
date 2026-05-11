package com.hrms.jeejateamozy.core.image

import android.content.Context
import coil.ImageLoader
import com.hrms.jeejateamozy.core.network.NetworkModule

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
            .okHttpClient(NetworkModule.okHttp)
            .crossfade(true)
            .build()
    }
}