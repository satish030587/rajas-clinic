package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.remote.ClinicProfileDto
import com.rajashomoeocare.clinic.domain.Appointment
import com.rajashomoeocare.clinic.domain.Language
import com.rajashomoeocare.clinic.domain.PatientRow
import com.rajashomoeocare.clinic.domain.TemplateKey
import com.rajashomoeocare.clinic.domain.renderTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** An appointment paired with the patient it belongs to, ready to message. */
data class ReminderRow(
    val appointment: Appointment,
    val patient: PatientRow,
)

data class AppointmentsState(
    val tomorrow: List<ReminderRow> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val sentIds: Set<String> = emptySet(),
)

/**
 * The day-before reminder list (spec §4.8). Booking itself happens on the
 * patient screen, where the doctor or reception already has the patient open.
 */
class AppointmentsViewModel(private val repo: ClinicRepository) : ViewModel() {

    private val _state = MutableStateFlow(AppointmentsState())
    val state: StateFlow<AppointmentsState> = _state.asStateFlow()

    private var templates: Map<Pair<TemplateKey, Language>, String> = emptyMap()
    private var clinic: ClinicProfileDto? = null

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }

        if (templates.isEmpty()) repo.templates().onSuccess { templates = it }
        if (clinic == null) repo.clinic().onSuccess { clinic = it }

        val due = repo.remindersDue().getOrElse { e ->
            _state.update { it.copy(loading = false, error = e.message) }
            return@launch
        }

        // The reminder endpoint returns appointments; pair each with its patient
        // so the list can show a name rather than an id.
        val patients = repo.patients().getOrDefault(emptyList())
        val rows = due.mapNotNull { appointment ->
            patients.firstOrNull { it.id == appointment.patientId }
                ?.let { ReminderRow(appointment, it) }
        }

        _state.update { it.copy(tomorrow = rows, loading = false) }
    }

    fun message(row: ReminderRow, key: TemplateKey): String? {
        val body = templates[key to row.patient.preferredLanguage]
            ?: templates[key to Language.EN]
            ?: return null
        return renderTemplate(
            body = body,
            patientName = row.patient.name,
            clinic = clinic,
            appointmentDate = row.appointment.date,
            lastVisit = row.patient.lastVisitDate,
        )
    }

    fun markSent(row: ReminderRow) = viewModelScope.launch {
        repo.logMessage(
            row.patient.id,
            TemplateKey.APPOINTMENT_REMINDER,
            row.patient.preferredLanguage,
        )
        _state.update { it.copy(sentIds = it.sentIds + row.appointment.id) }
    }

    companion object {
        fun tomorrow(): LocalDate = LocalDate.now().plusDays(1)
    }
}
