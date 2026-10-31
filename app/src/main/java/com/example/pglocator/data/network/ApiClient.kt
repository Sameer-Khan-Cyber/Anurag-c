package com.example.pglocator.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    const val DEFAULT_BASE_URL = "http://10.0.2.2/pg_locator/api/"

    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var cachedApiService: ApiService? = null

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun getService(baseUrl: String = currentBaseUrl): ApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (cachedApiService == null || currentBaseUrl != normalizedUrl) {
            currentBaseUrl = normalizedUrl
            cachedApiService = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ApiService::class.java)
        }
        return cachedApiService!!
    }

    fun updateBaseUrl(newUrl: String) {
        val trimmed = newUrl.trim()
        val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        currentBaseUrl = normalized
        cachedApiService = null
    }

    fun getBaseUrl(): String = currentBaseUrl
}
