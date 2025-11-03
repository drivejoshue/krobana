package com.example.orbanadrive.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body req: LoginReq): LoginRes

    @GET("auth/me")
    suspend fun me(): MeRes

    @POST("auth/logout") suspend fun logout(): Map<String, Any?>
}
