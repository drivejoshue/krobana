package com.example.orbanadrive.network

import com.example.orbanadrive.storage.TokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ApiClient(
    baseUrl: String,
    private val tokenStore: TokenStore
) {
    private val authInterceptor = Interceptor { chain ->
        val req = chain.request()
        val b = req.newBuilder().addHeader("Accept", "application/json")

        val token  = runBlocking { tokenStore.getToken() }
        val tenant = runBlocking { tokenStore.getTenantId() }
        if (!token.isNullOrBlank())  b.addHeader("Authorization", "Bearer $token")
        if (!tenant.isNullOrBlank()) b.addHeader("X-Tenant-ID", tenant)

        chain.proceed(b.build())
    }

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // 👇 Moshi con soporte para data classes de Kotlin
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val http = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logger)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl) // debe terminar con "/"
        .addConverterFactory(MoshiConverterFactory.create(moshi)) // 👈 usa el Moshi anterior
        .client(http)
        .build()

    fun <T> create(service: Class<T>): T = retrofit.create(service)
}
