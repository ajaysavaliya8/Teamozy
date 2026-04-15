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

    private const val DEFAULT_COMPANY_CODE = "jeejafashion"
    const val BASE_URL: String = "https://teamozy.com/data/$DEFAULT_COMPANY_CODE/m/"
    private const val PUBLIC_BASE_URL: String = "https://teamozy.com/"

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
     * Normalises the company URL path segment at request time.
     *
     * `companyCode` (e.g. "J_F") is a short identifier returned by find-company.
     * It is NOT the URL slug used in API paths. All app-built requests already use
     * DEFAULT_COMPANY_CODE in their URL (via BASE_URL), so no replacement is needed
     * for those. However, server-returned URLs (e.g. profile_url) may embed the short
     * code — those are rewritten to the default slug so they resolve correctly.
     */
    private val companyCodeInterceptor = Interceptor { chain ->
        val request = chain.request()
        val companyCode = PreferencesManager.getInstance(appContext).companyCode
            .takeIf { it.isNotBlank() && it != DEFAULT_COMPANY_CODE }
            ?: return@Interceptor chain.proceed(request)

        // Only act on server-provided URLs that contain the short code (e.g. profile_url)
        val originalPath = request.url.encodedPath
        if (!originalPath.contains("/data/$companyCode/m/")) {
            return@Interceptor chain.proceed(request)
        }

        val newPath = originalPath.replace("/data/$companyCode/m/", "/data/$DEFAULT_COMPANY_CODE/m/")
        val newUrl = request.url.newBuilder().encodedPath(newPath).build()
        chain.proceed(request.newBuilder().url(newUrl).build())
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
        if (res.code == 401) {
            AppStateManager.emitUnauthorized()
        } else if (res.code == 403) {
            // Only emit unauthorized for real auth failures.
            // "Invalid company code" is a URL-routing issue — do NOT logout.
            try {
                val body = res.peekBody(512).string()
                if (!body.contains("Invalid company code", ignoreCase = true)) {
                    AppStateManager.emitUnauthorized()
                }
            } catch (_: Exception) {
                AppStateManager.emitUnauthorized()
            }
        }
        res
    }

    // ✅ CHANGED: Make okHttp public so Coil can use it for authenticated image loading
    val okHttp by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .addInterceptor(companyCodeInterceptor)  // ← Swap company code dynamically
            .addInterceptor(authInterceptor)          // ← Add Bearer token
            .addInterceptor(logging)
            .addInterceptor(unauthorizedInterceptor)  // ← Handle 401
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

    // Public (no-auth) client for pre-login endpoints like find-company
    private val publicOkHttp by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val publicRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(PUBLIC_BASE_URL)
            .client(publicOkHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val publicApiService: PublicApiService by lazy { publicRetrofit.create(PublicApiService::class.java) }
}