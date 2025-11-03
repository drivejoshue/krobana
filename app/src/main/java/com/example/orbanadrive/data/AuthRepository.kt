package com.example.orbanadrive.repo

import com.example.orbanadrive.network.AuthApi
import com.example.orbanadrive.network.LoginReq
import com.example.orbanadrive.storage.TokenStore

class AuthRepository(
    private val api: AuthApi,
    private val tokenStore: TokenStore
) {
    suspend fun login(email: String, password: String): Boolean {
        val res = api.login(LoginReq(email, password))
        val token = res.token
        if (token.isNullOrBlank()) return false

        tokenStore.setToken(token)

        // opcional: precargar nombre para la UI
        runCatching { api.me() }.onSuccess { me ->
            tokenStore.setDriverName(me.user.name ?: me.driver?.name ?: "")
        }
        return true
    }

    /** Logout local (si luego agregas /auth/logout remoto, llámalo aquí con runCatching). */
    suspend fun logout() {
        // runCatching { api.remoteLogout() }
        tokenStore.setToken(null)
    }

    /** Alias “seguro” para la UI. */
    suspend fun logoutSafe() = logout()

    /** Debe ser suspend porque lee DataStore. */
    suspend fun hasValidSession(): Boolean {
        val token = tokenStore.getToken()
        return !token.isNullOrBlank()
    }
}
