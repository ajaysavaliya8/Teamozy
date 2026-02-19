package com.hrms.jeejateamozy.core.network

import android.content.Context
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    const val BASE_URL: String = "https://teamozy.com/data/jeejafashion/m/"

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
     * ✅ UPDATED: Also adds token as query parameter for file URLs (required by backend)
     */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        // Skip adding token for login/register endpoints
        val skipAuth = url.contains("send-login") || url.contains("verify-login")

        if (skipAuth) {
            chain.proceed(originalRequest)
        } else {
            // Add Authorization header with Bearer token AND X-Device-Id header
            val token = PreferencesManager.getInstance(appContext).authToken
            val deviceId = PreferencesManager.getInstance(appContext).deviceId

            // ✅ Check if this is a file URL that needs token as query parameter
            val isFileUrl = url.contains("/panel/files/") ||
                    url.contains("/files/profile/") ||
                    url.contains("/files/adhar/") ||
                    url.contains("/files/pan/") ||
                    url.contains("/files/company_logo/")

            val newRequest = if (isFileUrl && !token.isNullOrBlank()) {
                // ✅ For file URLs: Add token as query parameter (backend requirement)
                val newUrl = originalRequest.url.newBuilder()
                    .addQueryParameter("token", token)
                    .build()

                originalRequest.newBuilder()
                    .url(newUrl)
                    .header("Authorization", "Bearer $token")
                    .apply {
                        if (deviceId.isNotBlank()) {
                            header("X-Device-Id", deviceId)
                        }
                    }
                    .build()
            } else {
                // ✅ For regular API calls: Use Authorization header only
                originalRequest.newBuilder()
                    .apply {
                        if (!token.isNullOrBlank()) {
                            header("Authorization", "Bearer $token")
                        }
                        if (deviceId.isNotBlank()) {
                            header("X-Device-Id", deviceId)
                        }
                    }
                    .build()
            }

            chain.proceed(newRequest)
        }
    }

    private val unauthorizedInterceptor = Interceptor { chain ->
        val res = chain.proceed(chain.request())
        if (res.code == 401 || res.code == 403) {
            AppStateManager.emitUnauthorized()
        }
        res
    }

    // ✅ CHANGED: Make okHttp public so Coil can use it for authenticated image loading
    val okHttp by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .addInterceptor(authInterceptor)        // ← Add Bearer token
            .addInterceptor(logging)
            .addInterceptor(unauthorizedInterceptor) // ← Handle 401
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}