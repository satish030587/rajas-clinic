package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.WriteOutcome
import com.rajashomoeocare.clinic.data.remote.ClinicProfileDto
import com.rajashomoeocare.clinic.domain.Card
import com.rajashomoeocare.clinic.domain.Language
import com.rajashomoeocare.clinic.domain.Patient
import com.rajashomoeocare.clinic.domain.TemplateKey
import com.rajashomoeocare.clinic.domain.Visit
import com.rajashomoeocare.clinic.domain.renderTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PatientDetailState(
    val patient: Patient? = null,
    val visits: List<Visit> = emptyList(),
    val cards: List<Card> = emptyList(),
    val clinic: ClinicProfileDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val notice: String? = null,
) {
    val nextDue: LocalDate? get() = visits.firstOrNull()?.nextVisitDue
    fun card(id: String?) = cards.firstOrNull { it.id == id }
}

class PatientDetailViewModel(
    private val repo: ClinicRepository,
    private val patientId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientDetailState())
    val state: StateFlow<PatientDetailState> = _state.asStateFlow()

    private var templates: Map<Pair<TemplateKey, Language>, String> = emptyMap()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }

        repo.patient(patientId)
            .onSuccess { p -> _state.update { it.copy(patient = p, error = null) } }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }

        repo.visitsForPatient(patientId).onSuccess { visits ->
            _state.update { it.copy(visits = visits) }
        }
        repo.cards().onSuccess { cards -> _state.update { it.copy(cards = cards) } }
        repo.clinic().onSuccess { c -> _state.update { it.copy(clinic = c) } }
        if (templates.isEmpty()) repo.templates().onSuccess { templates = it }

        _state.update { it.copy(loading = false) }
    }

    fun startVisit(onQueued: (String) -> Unit) = viewModelScope.launch {
        when (val outcome = repo.startVisit(patientId, null)) {
            is WriteOutcome.Synced -> onQueued(outcome.visit.id)
            WriteOutcome.Queued -> _state.update {
                it.copy(notice = "Saved offline. It will sync when the network returns.")
            }
            is WriteOutcome.Failed -> _state.update { it.copy(error = outcome.message) }
        }
    }

    fun message(key: TemplateKey, appointmentDate: LocalDate? = null): String? {
        val patient = _state.value.patient ?: return null
        val body = templates[key to patient.preferredLanguage]
            ?: templates[key to Language.EN]
            ?: return null
        return renderTemplate(
            body = body,
            patientName = patient.name,
            clinic = _state.value.clinic,
            dueDate = _state.value.nextDue,
            lastVisit = _state.value.visits.firstOrNull()?.visitDate,
            appointmentDate = appointmentDate,
        )
    }

    /** The card's own text, for sending to the patient (spec §4.4). */
    fun cardMessage(cardId: String?): String? {
        val patient = _state.value.patient ?: return null
        return _state.value.card(cardId)?.body(patient.preferredLanguage)
    }

    fun markSent(key: TemplateKey) = viewModelScope.launch {
        val patient = _state.value.patient ?: return@launch
        repo.logMessage(patient.id, key, patient.preferredLanguage)
    }

    fun bookAppointment(date: LocalDate) = viewModelScope.launch {
        repo.bookAppointment(patientId, date)
            .onSuccess { _state.update { s -> s.copy(notice = "Appointment booked") } }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    fun clearNotice() = _state.update { it.copy(notice = null, error = null) }
}
