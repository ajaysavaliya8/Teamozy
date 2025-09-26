package com.example.teamozy.core.network
import com.example.teamozy.core.state.AppStateManager

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    const val BASE_URL: String = "https://teamozy.com/m/"

    private val logging by lazy {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    private val headersInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("Accept", "application/json")
            .build()
        chain.proceed(req)
    }

    private val unauthorizedInterceptor = Interceptor { chain ->
        val res = chain.proceed(chain.request())
        if (res.code == 401) {
            AppStateManager.emitUnauthorized()
        }
        res
    }
    private val okHttp by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .addInterceptor(logging)
            .addInterceptor(unauthorizedInterceptor) // 👈 add this
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create()) // ⬅️ gson
            .build()
    }

    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}
