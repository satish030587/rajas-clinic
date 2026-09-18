package com.rajashomoeocare.clinic.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore("clinic_settings")

class SettingsStore(private val context: Context) {

    private val pinHashKey = stringPreferencesKey("pin_hash")
    private val biometricKey = booleanPreferencesKey("biometric_enabled")
    private val defaultFeeKey = intPreferencesKey("default_consultation_fee")

    val hasPin: Flow<Boolean> =
        context.dataStore.data.map { it[pinHashKey] != null }

    val biometricEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[biometricKey] ?: false }

    val defaultConsultationFee: Flow<Int> =
        context.dataStore.data.map { it[defaultFeeKey] ?: 200 }

    suspend fun setPin(pin: String) {
        context.dataStore.edit { it[pinHashKey] = hash(pin) }
    }

    suspend fun checkPin(pin: String): Boolean {
        val stored = context.dataStore.data.first()[pinHashKey] ?: return false
        return stored == hash(pin)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[biometricKey] = enabled }
    }

    suspend fun setDefaultConsultationFee(amount: Int) {
        context.dataStore.edit { it[defaultFeeKey] = amount }
    }

    // A 4-digit PIN has a trivial keyspace, so this hash only keeps the PIN out of
    // plaintext on disk. The real protection is the OS lock plus optional biometrics.
    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("rajas::$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }
}
