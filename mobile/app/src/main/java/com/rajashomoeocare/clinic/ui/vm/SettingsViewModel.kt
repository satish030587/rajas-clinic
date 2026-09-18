package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.SettingsStore
import com.rajashomoeocare.clinic.data.local.Language
import com.rajashomoeocare.clinic.data.local.MessageTemplateEntity
import com.rajashomoeocare.clinic.data.local.TemplateKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class SettingsState(
    val templates: List<MessageTemplateEntity> = emptyList(),
    val defaultFee: Int = 200,
    val biometricEnabled: Boolean = false,
    val patientCount: Int = 0,
) {
    fun template(key: TemplateKey, language: Language): MessageTemplateEntity? =
        templates.firstOrNull { it.templateKey == key && it.language == language }
}

class SettingsViewModel(
    private val repo: ClinicRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(
        repo.templates.observeAll(),
        settings.defaultConsultationFee,
        settings.biometricEnabled,
        repo.patients.observeCount(),
    ) { templates, fee, biometric, count ->
        SettingsState(templates, fee, biometric, count)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun saveTemplate(key: TemplateKey, language: Language, body: String) = viewModelScope.launch {
        repo.templates.upsert(
            MessageTemplateEntity(
                templateKey = key,
                language = language,
                bodyText = body,
                updatedAt = Instant.now(),
            )
        )
    }

    fun setDefaultFee(amount: Int) = viewModelScope.launch {
        settings.setDefaultConsultationFee(amount)
    }

    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setBiometricEnabled(enabled)
    }

    fun setPin(pin: String) = viewModelScope.launch {
        settings.setPin(pin)
    }
}
