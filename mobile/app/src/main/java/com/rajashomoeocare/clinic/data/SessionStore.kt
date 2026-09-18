package com.rajashomoeocare.clinic.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.sessionStore by preferencesDataStore("clinic_session")

enum class UserRole { DOCTOR, RECEPTION, PHARMACY;

    companion object {
        fun from(value: String?): UserRole = when (value?.lowercase()) {
            "doctor" -> DOCTOR
            "pharmacy" -> PHARMACY
            else -> RECEPTION
        }
    }
}

data class Session(
    val token: String,
    val userId: String,
    val displayName: String,
    val role: UserRole,
    val uiLanguage: String,
)

class SessionStore(private val context: Context) {

    private val tokenKey = stringPreferencesKey("token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val nameKey = stringPreferencesKey("display_name")
    private val roleKey = stringPreferencesKey("role")
    private val languageKey = stringPreferencesKey("ui_language")

    val session: Flow<Session?> = context.sessionStore.data.map { prefs ->
        val token = prefs[tokenKey] ?: return@map null
        Session(
            token = token,
            userId = prefs[userIdKey].orEmpty(),
            displayName = prefs[nameKey].orEmpty(),
            role = UserRole.from(prefs[roleKey]),
            uiLanguage = prefs[languageKey] ?: "en",
        )
    }

    suspend fun save(
        token: String,
        userId: String,
        displayName: String,
        role: String,
        uiLanguage: String,
    ) {
        context.sessionStore.edit {
            it[tokenKey] = token
            it[userIdKey] = userId
            it[nameKey] = displayName
            it[roleKey] = role
            it[languageKey] = uiLanguage
        }
    }

    suspend fun setLanguage(language: String) {
        context.sessionStore.edit { it[languageKey] = language }
    }

    suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }

    suspend fun currentLanguage(): String =
        context.sessionStore.data.first()[languageKey] ?: "en"

    /**
     * Read synchronously for the OkHttp interceptor, which is not a coroutine.
     * Cheap: DataStore keeps the last value in memory after first read.
     */
    fun tokenBlocking(): String? = runBlocking {
        context.sessionStore.data.first()[tokenKey]
    }
}
