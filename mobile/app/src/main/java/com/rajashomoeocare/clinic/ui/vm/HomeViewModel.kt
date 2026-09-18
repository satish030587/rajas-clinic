package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.remote.ClinicProfileDto
import com.rajashomoeocare.clinic.domain.Language
import com.rajashomoeocare.clinic.domain.PatientRow
import com.rajashomoeocare.clinic.domain.RecallBucket
import com.rajashomoeocare.clinic.domain.RecallItem
import com.rajashomoeocare.clinic.domain.TemplateKey
import com.rajashomoeocare.clinic.domain.TodaySummary
import com.rajashomoeocare.clinic.domain.Visit
import com.rajashomoeocare.clinic.domain.renderTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeState(
    val summary: TodaySummary = TodaySummary(),
    val queue: List<Visit> = emptyList(),
    val queueNames: Map<String, PatientRow> = emptyMap(),
    val loading: Boolean = true,
    val error: String? = null,
) {
    val overdueCount: Int get() = summary.overdue.size
    fun overdueBy(bucket: RecallBucket) = summary.overdue.filter { it.bucket == bucket }
}

/** Backs the Today screen, the waiting queue and the Recall screen. */
class HomeViewModel(private val repo: ClinicRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var templates: Map<Pair<TemplateKey, Language>, String> = emptyMap()
    private var clinic: ClinicProfileDto? = null

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }

        val summary = repo.today()
        summary.onFailure { e ->
            _state.update { it.copy(loading = false, error = e.message) }
        }
        summary.onSuccess { data ->
            _state.update { it.copy(summary = data, loading = false, error = null) }
        }

        // The queue is only populated for staff who can see it; a failure here
        // must not blank out the recall list, which is the more important view.
        repo.queue().onSuccess { visits ->
            val names = mutableMapOf<String, PatientRow>()
            val known = (state.value.summary.dueToday.map { it.patient } +
                state.value.summary.seenToday)
            visits.forEach { visit ->
                known.firstOrNull { it.id == visit.patientId }?.let {
                    names[visit.patientId] = it
                }
            }
            val missing = visits.map { it.patientId }.filter { it !in names }
            if (missing.isNotEmpty()) {
                repo.patients().onSuccess { all ->
                    all.filter { it.id in missing }.forEach { names[it.id] = it }
                }
            }
            _state.update { it.copy(queue = visits, queueNames = names) }
        }

        if (templates.isEmpty()) repo.templates().onSuccess { templates = it }
        if (clinic == null) repo.clinic().onSuccess { clinic = it }
    }

    /** Builds a message in the patient's own language (spec §7). */
    fun message(
        key: TemplateKey,
        patient: PatientRow,
        dueDate: LocalDate? = null,
        appointmentDate: LocalDate? = null,
    ): String? {
        val body = templates[key to patient.preferredLanguage]
            ?: templates[key to Language.EN]
            ?: return null
        return renderTemplate(
            body = body,
            patientName = patient.name,
            clinic = clinic,
            dueDate = dueDate,
            lastVisit = patient.lastVisitDate,
            appointmentDate = appointmentDate,
        )
    }

    fun markSent(item: RecallItem, key: TemplateKey) = viewModelScope.launch {
        repo.logMessage(item.patient.id, key, item.patient.preferredLanguage)
    }
}
