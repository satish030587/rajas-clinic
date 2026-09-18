package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.domain.Billing
import com.rajashomoeocare.clinic.domain.Card
import com.rajashomoeocare.clinic.domain.Medicine
import com.rajashomoeocare.clinic.domain.MedicineForm
import com.rajashomoeocare.clinic.domain.Patient
import com.rajashomoeocare.clinic.domain.PaymentMode
import com.rajashomoeocare.clinic.domain.Visit
import com.rajashomoeocare.clinic.domain.Vitals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MedicineDraft(
    val name: String = "",
    val potency: String = "",
    val form: MedicineForm = MedicineForm.PILLS,
    val quantity: String = "",
) {
    val isBlank: Boolean get() = name.isBlank()
    fun toMedicine() = Medicine(
        name = name.trim(),
        potency = potency.takeIf(String::isNotBlank),
        form = form,
        quantity = quantity.takeIf(String::isNotBlank),
    )
}

data class VisitEditorState(
    val visit: Visit? = null,
    val patient: Patient? = null,
    val vitals: Vitals? = null,
    val complaint: String = "",
    val medicines: List<MedicineDraft> = listOf(MedicineDraft()),
    val cards: List<Card> = emptyList(),
    val selectedCardId: String? = null,
    val nextVisitDue: LocalDate? = LocalDate.now().plusDays(15),
    val consultationFee: String = "",
    val medicineCharge: String = "",
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val paid: Boolean = true,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
) {
    val total: Int
        get() = (consultationFee.toIntOrNull() ?: 0) + (medicineCharge.toIntOrNull() ?: 0)

    val selectedCard: Card? get() = cards.firstOrNull { it.id == selectedCardId }
}

class VisitEditorViewModel(
    private val repo: ClinicRepository,
    private val visitId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(VisitEditorState())
    val state: StateFlow<VisitEditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Opening the visit moves it out of the waiting queue.
            val visit = repo.openVisit(visitId).getOrElse { e ->
                _state.update { it.copy(loading = false, error = e.message) }
                return@launch
            }

            val cards = repo.cards().getOrDefault(emptyList())
            val patient = repo.patient(visit.patientId).getOrNull()
            val defaultFee = repo.clinic().getOrNull()?.defaultConsultationFee ?: 200

            val existing = visit.medicines.map {
                MedicineDraft(
                    name = it.name,
                    potency = it.potency.orEmpty(),
                    form = it.form,
                    quantity = it.quantity.orEmpty(),
                )
            }

            _state.update {
                it.copy(
                    visit = visit,
                    patient = patient,
                    vitals = visit.vitals,
                    complaint = visit.complaint.orEmpty(),
                    medicines = existing.ifEmpty { listOf(MedicineDraft()) },
                    cards = cards,
                    selectedCardId = visit.cardId,
                    nextVisitDue = visit.nextVisitDue ?: LocalDate.now().plusDays(15),
                    consultationFee = (visit.billing?.consultationFee ?: defaultFee).toString(),
                    medicineCharge = visit.billing?.medicineCharge?.toString().orEmpty(),
                    paymentMode = visit.billing?.paymentMode ?: PaymentMode.CASH,
                    paid = visit.billing?.paid ?: true,
                    loading = false,
                )
            }
        }
    }

    fun edit(transform: (VisitEditorState) -> VisitEditorState) =
        _state.update { transform(it).copy(error = null) }

    fun addMedicine() = _state.update { it.copy(medicines = it.medicines + MedicineDraft()) }

    fun updateMedicine(index: Int, draft: MedicineDraft) = _state.update { s ->
        s.copy(medicines = s.medicines.toMutableList().also { it[index] = draft })
    }

    fun removeMedicine(index: Int) = _state.update { s ->
        val next = s.medicines.toMutableList().also { it.removeAt(index) }
        s.copy(medicines = next.ifEmpty { listOf(MedicineDraft()) })
    }

    suspend fun save(complete: Boolean): Boolean {
        val s = _state.value
        _state.update { it.copy(saving = true, error = null) }

        val result = repo.saveClinical(
            visitId = visitId,
            complaint = s.complaint.takeIf(String::isNotBlank),
            cardId = s.selectedCardId,
            nextVisitDue = s.nextVisitDue,
            medicines = s.medicines.filterNot { it.isBlank }.map { it.toMedicine() },
            billing = Billing(
                consultationFee = s.consultationFee.toIntOrNull() ?: 0,
                medicineCharge = s.medicineCharge.toIntOrNull() ?: 0,
                paymentMode = s.paymentMode,
                paid = s.paid,
            ),
            complete = complete,
        )

        return result.fold(
            onSuccess = {
                _state.update { st -> st.copy(saving = false, visit = it) }
                true
            },
            onFailure = { e ->
                _state.update { it.copy(saving = false, error = e.message) }
                false
            },
        )
    }
}
