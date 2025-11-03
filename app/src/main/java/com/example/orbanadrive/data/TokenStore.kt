package com.example.orbanadrive.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore("orbana_prefs")

class TokenStore(private val context: Context) {
    private val KEY_TOKEN    = stringPreferencesKey("auth_token")
    private val KEY_NAME     = stringPreferencesKey("driver_name")
    private val KEY_TENANTID = stringPreferencesKey("tenant_id")     // si lo usas
    private val KEY_REMEMBER = booleanPreferencesKey("remember_me")
    private val KEY_EMAIL    = stringPreferencesKey("saved_email")

    // ===== token =====
    suspend fun setToken(token: String?) {
        context.dataStore.edit { it[KEY_TOKEN] = token ?: "" }
    }
    suspend fun getToken(): String? {
        val prefs = context.dataStore.data.first()
        val t = prefs[KEY_TOKEN]
        return if (t.isNullOrBlank()) null else t
    }

    // ===== nombre opcional =====
    suspend fun setDriverName(name: String?) {
        context.dataStore.edit { it[KEY_NAME] = name ?: "" }
    }


    // ===== tenant opcional =====

    suspend fun getTenantId(): String? {
        val prefs = context.dataStore.data.first()
        val t = prefs[KEY_TENANTID]
        return if (t.isNullOrBlank()) null else t
    }

    // ===== remember/email =====
    suspend fun setRemember(remember: Boolean) {
        context.dataStore.edit { it[KEY_REMEMBER] = remember }
    }
    suspend fun getRemember(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_REMEMBER] ?: false
    }
    suspend fun setSavedEmail(email: String?) {
        context.dataStore.edit { it[KEY_EMAIL] = email ?: "" }
    }
    suspend fun getSavedEmail(): String? {
        val prefs = context.dataStore.data.first()
        val e = prefs[KEY_EMAIL]
        return if (e.isNullOrBlank()) null else e
    }

    // ===== borrar todo =====
    suspend fun clear() {
        context.dataStore.edit { it.clear() }   // <- limpia todas las claves
    }
}
