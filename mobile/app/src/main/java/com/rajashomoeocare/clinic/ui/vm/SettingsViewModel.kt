package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.SessionStore
import com.rajashomoeocare.clinic.data.remote.ClinicProfileDto
import com.rajashomoeocare.clinic.domain.Card
import com.rajashomoeocare.clinic.domain.Language
import com.rajashomoeocare.clinic.domain.TemplateKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val templates: Map<Pair<TemplateKey, Language>, String> = emptyMap(),
    val cards: List<Card> = emptyList(),
    val clinic: ClinicProfileDto? = null,
    val clinicName: String = "",
    val displayName: String = "",
    val role: String = "",
    val uiLanguage: Language = Language.EN,
    val loading: Boolean = true,
    val notice: String? = null,
    val error: String? = null,
)

class SettingsViewModel(
    private val repo: ClinicRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            session.session.collect { s ->
                if (s != null) {
                    _state.update {
                        it.copy(
                            displayName = s.displayName,
                            role = s.role.name.lowercase()
                                .replaceFirstChar(Char::uppercase),
                            uiLanguage = Language.from(s.uiLanguage),
                        )
                    }
                }
            }
        }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        repo.templates().onSuccess { t -> _state.update { it.copy(templates = t) } }
        repo.cards().onSuccess { c -> _state.update { it.copy(cards = c) } }
        repo.clinic().onSuccess { c ->
            _state.update { it.copy(clinic = c, clinicName = c.name) }
        }
        _state.update { it.copy(loading = false) }
    }

    /**
     * Clinic details live on the server so both devices — and every message
     * template — read the same address, hours and links (spec §4.9).
     */
    fun saveClinic(profile: ClinicProfileDto) = viewModelScope.launch {
        repo.updateClinic(profile)
            .onSuccess { saved ->
                _state.update {
                    it.copy(clinic = saved, clinicName = saved.name, notice = "Saved")
                }
            }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    fun template(key: TemplateKey, language: Language): String =
        _state.value.templates[key to language].orEmpty()

    fun saveTemplate(key: TemplateKey, language: Language, body: String) =
        viewModelScope.launch {
            repo.updateTemplate(key, language, body)
                .onSuccess {
                    _state.update {
                        it.copy(
                            templates = it.templates + ((key to language) to body),
                            notice = "Saved",
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }

    /**
     * Spec §4.7: the app's own language is a per-user setting, stored on the
     * server so it follows the user to whichever device they sign in on.
     */
    fun setUiLanguage(language: Language) = viewModelScope.launch {
        session.setLanguage(language.wire)
        _state.update { it.copy(uiLanguage = language) }
        repo.setUiLanguage(language)
    }

    fun signOut(onDone: () -> Unit) = viewModelScope.launch {
        session.clear()
        onDone()
    }

    fun clearNotice() = _state.update { it.copy(notice = null, error = null) }
}
