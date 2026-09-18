package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.SettingsStore
import com.rajashomoeocare.clinic.data.local.InvoiceEntity
import com.rajashomoeocare.clinic.data.local.PatientEntity
import com.rajashomoeocare.clinic.data.local.PaymentMode
import com.rajashomoeocare.clinic.data.local.PhotoEntity
import com.rajashomoeocare.clinic.data.local.VisitEntity
import com.rajashomoeocare.clinic.data.local.newId
import com.rajashomoeocare.clinic.util.deletePhotoFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class VisitFormState(
    val visitId: String = newId(),
    val patient: PatientEntity? = null,
    val visitDate: LocalDate = LocalDate.now(),
    val complaint: String = "",
    val remedy: String = "",
    val potency: String = "",
    val advice: String = "",
    val nextVisitDue: LocalDate? = LocalDate.now().plusDays(15),
    val consultationFee: String = "",
    val medicineCharge: String = "",
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val paid: Boolean = true,
    /** Photos already persisted against this visit (edit mode only). */
    val photos: List<PhotoEntity> = emptyList(),
    /** Photos taken in this session, written only when the visit is saved. */
    val pendingPhotos: List<String> = emptyList(),
    val priorPhotos: List<PhotoEntity> = emptyList(),
    val isNew: Boolean = true,
    val saving: Boolean = false,
) {
    val total: Int
        get() = (consultationFee.toIntOrNull() ?: 0) + (medicineCharge.toIntOrNull() ?: 0)
}

class VisitEditorViewModel(
    private val repo: ClinicRepository,
    private val settings: SettingsStore,
    private val patientId: String,
    private val existingVisitId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(VisitFormState())
    val state: StateFlow<VisitFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val patient = repo.patients.get(patientId)
            val priorPhotos = repo.photos.observeForPatient(patientId).first()

            if (existingVisitId != null) {
                val visit = repo.visits.get(existingVisitId)
                val invoice = repo.invoices.forVisit(existingVisitId)
                val photos = repo.photos.observeForVisit(existingVisitId).first()
                if (visit != null) {
                    _state.value = VisitFormState(
                        visitId = visit.id,
                        patient = patient,
                        visitDate = visit.visitDate,
                        complaint = visit.complaint.orEmpty(),
                        remedy = visit.remedyGiven.orEmpty(),
                        potency = visit.potency.orEmpty(),
                        advice = visit.adviceGiven.orEmpty(),
                        nextVisitDue = visit.nextVisitDue,
                        consultationFee = invoice?.consultationFee?.toString().orEmpty(),
                        medicineCharge = invoice?.medicineCharge?.toString().orEmpty(),
                        paymentMode = invoice?.paymentMode ?: PaymentMode.CASH,
                        paid = invoice?.paid ?: true,
                        photos = photos,
                        priorPhotos = priorPhotos.filterNot { it.visitId == visit.id },
                        isNew = false,
                    )
                    return@launch
                }
            }

            _state.update {
                it.copy(
                    patient = patient,
                    priorPhotos = priorPhotos,
                    consultationFee = settings.defaultConsultationFee.first().toString(),
                )
            }
        }
    }

    fun edit(transform: (VisitFormState) -> VisitFormState) {
        _state.update(transform)
    }

    fun addPhoto(filePath: String) {
        _state.update { it.copy(pendingPhotos = it.pendingPhotos + filePath) }
    }

    fun removePendingPhoto(filePath: String) {
        deletePhotoFile(filePath)
        _state.update { it.copy(pendingPhotos = it.pendingPhotos - filePath) }
    }

    fun removeSavedPhoto(photo: PhotoEntity) = viewModelScope.launch {
        repo.photos.delete(photo)
        deletePhotoFile(photo.filePath)
        _state.update { it.copy(photos = it.photos - photo) }
    }

    suspend fun save(): Boolean {
        val s = _state.value
        _state.update { it.copy(saving = true) }

        val visit = VisitEntity(
            id = s.visitId,
            patientId = patientId,
            visitDate = s.visitDate,
            complaint = s.complaint.takeIf(String::isNotBlank),
            remedyGiven = s.remedy.takeIf(String::isNotBlank),
            potency = s.potency.takeIf(String::isNotBlank),
            adviceGiven = s.advice.takeIf(String::isNotBlank),
            nextVisitDue = s.nextVisitDue,
        )
        val invoice = InvoiceEntity(
            visitId = s.visitId,
            consultationFee = s.consultationFee.toIntOrNull() ?: 0,
            medicineCharge = s.medicineCharge.toIntOrNull() ?: 0,
            paymentMode = s.paymentMode,
            paid = s.paid,
            paidAt = if (s.paid) Instant.now() else null,
        )
        repo.saveVisit(visit, invoice, newPhotoPaths = s.pendingPhotos)
        _state.update { it.copy(saving = false, pendingPhotos = emptyList()) }
        return true
    }

    /** Photo files copied in but never saved would otherwise leak into storage. */
    fun discardUnsaved() {
        _state.value.pendingPhotos.forEach(::deletePhotoFile)
    }
}
