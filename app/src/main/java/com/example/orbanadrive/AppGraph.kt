package com.example.orbanadrive

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.orbanadrive.network.ApiClient
import com.example.orbanadrive.network.AuthApi
import com.example.orbanadrive.network.DriverApi
import com.example.orbanadrive.repo.AuthRepository
import com.example.orbanadrive.repo.DriverRepository
import com.example.orbanadrive.storage.TokenStore

class AppGraph(ctx: Context) {
    val tokenStore = TokenStore(ctx)
    private val apiClient = ApiClient(BuildConfig.API_BASE_URL, tokenStore)

    fun <T> api(service: Class<T>): T = apiClient.create(service)
    val authApi: AuthApi   = apiClient.create(AuthApi::class.java)
    val driverApi: DriverApi = apiClient.create(DriverApi::class.java)

    val authRepo  = AuthRepository(authApi, tokenStore)
    val driverRepo = DriverRepository(authApi, driverApi)
}

val LocalAppGraph = staticCompositionLocalOf<AppGraph> {
    error("LocalAppGraph not provided")
}
