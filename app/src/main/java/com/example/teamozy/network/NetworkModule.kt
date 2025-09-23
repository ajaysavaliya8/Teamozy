package com.example.teamozy.network

object NetworkModule {

    private const val BASE_URL = "https://teamozy.com/m/"

    private val loggingInterceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val retrofit = retrofit2.Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}