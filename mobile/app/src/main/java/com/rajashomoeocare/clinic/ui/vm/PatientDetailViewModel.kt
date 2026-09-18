package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.local.MessageLogEntity
import com.rajashomoeocare.clinic.data.local.PatientEntity
import com.rajashomoeocare.clinic.data.local.PhotoEntity
import com.rajashomoeocare.clinic.data.local.TemplateKey
import com.rajashomoeocare.clinic.data.local.VisitWithBilling
import com.rajashomoeocare.clinic.domain.renderTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PatientDetailState(
    val patient: PatientEntity? = null,
    val visits: List<VisitWithBilling> = emptyList(),
    val photos: List<PhotoEntity> = emptyList(),
    val messages: List<MessageLogEntity> = emptyList(),
) {
    val nextDue get() = visits.firstOrNull()?.nextVisitDue
    val totalBilled get() = visits.sumOf { (it.consultationFee ?: 0) + (it.medicineCharge ?: 0) }
}

class PatientDetailViewModel(
    private val repo: ClinicRepository,
    private val patientId: String,
) : ViewModel() {

    val state: StateFlow<PatientDetailState> = combine(
        repo.patients.observe(patientId),
        repo.visits.observeForPatient(patientId),
        repo.photos.observeForPatient(patientId),
        repo.messageLog.observeForPatient(patientId),
    ) { patient, visits, photos, messages ->
        PatientDetailState(patient, visits, photos, messages)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatientDetailState())

    suspend fun message(key: TemplateKey): String? {
        val patient = state.value.patient ?: return null
        val template = repo.template(key, patient.preferredLanguage) ?: return null
        return renderTemplate(
            body = template.bodyText,
            patientName = patient.name,
            dueDate = state.value.nextDue,
            lastVisit = state.value.visits.firstOrNull()?.visitDate,
        )
    }

    suspend fun markSent(key: TemplateKey) {
        val patient = state.value.patient ?: return
        repo.recordMessageSent(patient.id, key, patient.preferredLanguage)
    }

    fun archive() = viewModelScope.launch {
        repo.patients.archive(patientId)
    }
}
