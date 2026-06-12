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
        HttpLoggingInterceptor().apply {
            level = if (com.hrms.jeejateamozy.BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }
    }

    private val headersInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("Accept", "application/json")
            .build()
        chain.proceed(req)
    }

    /**
     * Rewrites the company URL slug at request time using the code returned by
     * find-company and stored in PreferencesManager.
     *
     * Retrofit builds every request from BASE_URL (which contains DEFAULT_COMPANY_CODE).
     * After find-company runs, `pm.companyCode` holds the tenant's slug — we swap
     * `/data/$DEFAULT_COMPANY_CODE/m/` in the outgoing path for `/data/$savedCode/m/`
     * so all endpoints (send-login, verify-login, etc.) hit the correct tenant.
     *
     * Pre-login (blank prefs) the default slug is kept as-is.
     */
    private val companyCodeInterceptor = Interceptor { chain ->
        val request = chain.request()
        val savedCode = PreferencesManager.getInstance(appContext).companyCode
            .takeIf { it.isNotBlank() && it != DEFAULT_COMPANY_CODE }
            ?: return@Interceptor chain.proceed(request)

        val originalPath = request.url.encodedPath
        val defaultSegment = "/data/$DEFAULT_COMPANY_CODE/m/"
        if (!originalPath.contains(defaultSegment)) {
            return@Interceptor chain.proceed(request)
        }

        val newPath = originalPath.replace(defaultSegment, "/data/$savedCode/m/")
        val newUrl = request.url.newBuilder().encodedPath(newPath).build()
        chain.proceed(request.newBuilder().url(newUrl).build())
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        val skipAuth = url.contains("send-login") || url.contains("verify-login")
                || url.contains("forgot-password") || url.contains("verify-reset-otp")
                || url.contains("reset-password")

        if (skipAuth) {
            chain.proceed(originalRequest)
        } else {
            val token = PreferencesManager.getInstance(appContext).authToken
            val deviceId = PreferencesManager.getInstance(appContext).deviceId

            val newRequest = originalRequest.newBuilder()
                .apply {
                    if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
                    if (deviceId.isNotBlank()) header("X-Device-Id", deviceId)
                }
                .build()

            chain.proceed(newRequest)
        }
    }

    // Endpoints hit before the user is authenticated. 401/403 from these must never
    // trigger a session-expired flow (clearAll + redirect) — they're business errors
    // (e.g. "Employment is not active", "Mobile not registered") on public routes.
    private val preAuthPathSuffixes = listOf(
        "/send-login",
        "/verify-login",
        "/forgot-password",
        "/verify-reset-otp",
        "/reset-password",
        "/send-change-device-otp",
        "/request-change-device",
    )

    private val unauthorizedInterceptor = Interceptor { chain ->
        val req = chain.request()
        val res = chain.proceed(req)

        val path = req.url.encodedPath
        val isPreAuth = preAuthPathSuffixes.any { path.endsWith(it) }

        if (!isPreAuth && (res.code == 401 || res.code == 403)) {
            // Distinguish true auth-middleware 401/403 from app-level business errors.
            // App-level errors use the structured envelope {success, message, error_code, ...}
            // — those should NOT log the user out (e.g. "Shift not allocated",
            // "Face recognition is not enabled", "Employment is not active").
            // Also preserve the existing "Invalid company code" allowance.
            try {
                val body = res.peekBody(512).string()
                val isStructuredBusinessError = body.contains("\"error_code\"")
                val isCompanyCodeIssue = body.contains("Invalid company code", ignoreCase = true)
                if (!isStructuredBusinessError && !isCompanyCodeIssue) {
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