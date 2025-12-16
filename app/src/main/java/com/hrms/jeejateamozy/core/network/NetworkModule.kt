package com.hrms.jeejateamozy.core.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    const val BASE_URL: String = "https://teamozy.com/m/"

    // Must be initialized before use
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val logging by lazy {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    private val headersInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("Accept", "application/json")
            .build()
        chain.proceed(req)
    }

    /**
     * Adds Authorization: Bearer <token> header to all requests (except login endpoints)
     */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        // Skip adding token for login/register endpoints
        val skipAuth = url.contains("send-login") || url.contains("verify-login")

        if (skipAuth) {
            chain.proceed(originalRequest)
        } else {
            val token = PreferencesManager.getInstance(appContext).authToken
            val deviceId = PreferencesManager.getInstance(appContext).deviceId

            val newRequest = originalRequest.newBuilder()

            if (!token.isNullOrBlank()) {
                newRequest.header("Authorization", "Bearer $token")
            }

            if (deviceId.isNotBlank()) {
                newRequest.header("X-Device-Id", deviceId)
            }

            chain.proceed(newRequest.build())
        }
    }

    private val unauthorizedInterceptor = Interceptor { chain ->
        val res = chain.proceed(chain.request())
        if (res.code == 401) {
            AppStateManager.emitUnauthorized()
        }
        res
    }

    val okHttp by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .addInterceptor(unauthorizedInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(
                object : TypeToken<List<String>>() {}.type,
                StringToListAdapter()
            )
            .create()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}