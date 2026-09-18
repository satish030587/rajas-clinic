package com.rajashomoeocare.clinic.data.remote

import android.content.Context
import coil.ImageLoader
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.rajashomoeocare.clinic.BuildConfig
import com.rajashomoeocare.clinic.data.SessionStore
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun httpClient(sessionStore: SessionStore): OkHttpClient {
        val auth = Interceptor { chain ->
            val token = sessionStore.tokenBlocking()
            val request = if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(auth)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            // Short connect timeout on purpose: when the router is down, a save
            // should drop into the offline queue in a few seconds rather than
            // leaving the doctor staring at a disabled button mid-consultation.
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun create(
        client: OkHttpClient,
        baseUrl: String = BuildConfig.API_BASE_URL,
    ): ApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApiService::class.java)

    /**
     * Scan and report images live behind the same token as the rest of the API,
     * so Coil has to use the authenticated client rather than its own.
     */
    fun imageLoader(context: Context, client: OkHttpClient): ImageLoader =
        ImageLoader.Builder(context)
            .okHttpClient(client)
            .crossfade(true)
            .build()
}
