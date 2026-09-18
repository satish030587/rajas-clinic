package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.WriteOutcome
import com.rajashomoeocare.clinic.data.remote.PatientCreate
import com.rajashomoeocare.clinic.domain.Language
import com.rajashomoeocare.clinic.domain.PatientRow
import com.rajashomoeocare.clinic.domain.Sex
import com.rajashomoeocare.clinic.domain.Vitals
import com.rajashomoeocare.clinic.util.normalizePhone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FormField { NAME, PHONE, ALT_PHONE, AGE }

data class PatientFormState(
    val id: String? = null,
    val name: String = "",
    val phone: String = "",
    val alternatePhone: String = "",
    val sex: Sex = Sex.MALE,
    val ageText: String = "",
    val address: String = "",
    val occupation: String = "",
    val bloodGroup: String = "",
    val referredBy: String = "",
    val currentMedication: String = "",
    val language: Language = Language.EN,

    // Vitals, taken at the desk (spec §4.2)
    val heightText: String = "",
    val weightText: String = "",
    val systolicText: String = "",
    val diastolicText: String = "",
    val pulseText: String = "",

    /** Start a visit straight after saving — the normal reception flow. */
    val queueAfterSave: Boolean = true,

    val errors: Map<FormField, String> = emptyMap(),
    val duplicate: PatientRow? = null,
    val saving: Boolean = false,
    val queuedOffline: Boolean = false,
    val error: String? = null,
) {
    val isNew: Boolean get() = id == null

    val vitals: Vitals
        get() = Vitals(
            heightCm = heightText.toDoubleOrNull(),
            weightKg = weightText.toDoubleOrNull(),
            bpSystolic = systolicText.toIntOrNull(),
            bpDiastolic = diastolicText.toIntOrNull(),
            pulse = pulseText.toIntOrNull(),
        )
}

class PatientFormViewModel(
    private val repo: ClinicRepository,
    private val patientId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientFormState())
    val state: StateFlow<PatientFormState> = _state.asStateFlow()

    init {
        if (patientId != null) {
            viewModelScope.launch {
                repo.patient(patientId).onSuccess { p ->
                    _state.value = PatientFormState(
                        id = p.id,
                        name = p.name,
                        phone = p.phone,
                        alternatePhone = p.alternatePhone.orEmpty(),
                        sex = p.sex,
                        ageText = (p.ageYears ?: p.age)?.toString().orEmpty(),
                        address = p.address.orEmpty(),
                        occupation = p.occupation.orEmpty(),
                        bloodGroup = p.bloodGroup.orEmpty(),
                        referredBy = p.referredBy.orEmpty(),
                        currentMedication = p.currentMedication.orEmpty(),
                        language = p.preferredLanguage,
                        queueAfterSave = false,
                    )
                }
            }
        }
    }

    fun edit(transform: (PatientFormState) -> PatientFormState) {
        _state.update { transform(it).copy(errors = emptyMap(), error = null) }
    }

    fun dismissDuplicate() = _state.update { it.copy(duplicate = null) }

    /**
     * Checks the number as soon as it is complete, so the duplicate is caught
     * before the rest of the form is typed rather than after (spec §4.11).
     */
    fun checkPhone() {
        val phone = normalizePhone(_state.value.phone) ?: return
        if (!_state.value.isNew) return
        viewModelScope.launch {
            repo.patientByPhone(phone).onSuccess { existing ->
                _state.update { it.copy(duplicate = existing) }
            }
        }
    }

    /** Returns the saved patient id, or null when validation or the call failed. */
    suspend fun save(): String? {
        val s = _state.value
        val errors = buildMap {
            if (s.name.isBlank()) put(FormField.NAME, ERROR_REQUIRED)
            if (normalizePhone(s.phone) == null) put(FormField.PHONE, ERROR_PHONE)
            if (s.alternatePhone.isNotBlank() && normalizePhone(s.alternatePhone) == null) {
                put(FormField.ALT_PHONE, ERROR_PHONE)
            }
            val age = s.ageText.toIntOrNull()
            if (age == null || age !in 0..120) put(FormField.AGE, ERROR_AGE)
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return null
        }

        _state.update { it.copy(saving = true, error = null) }

        val body = PatientCreate(
            name = s.name.trim().split(Regex("\\s+")).joinToString(" "),
            phone = normalizePhone(s.phone)!!,
            sex = s.sex.wire,
            alternatePhone = s.alternatePhone.takeIf(String::isNotBlank)
                ?.let(::normalizePhone),
            ageYears = s.ageText.toIntOrNull(),
            address = s.address.takeIf(String::isNotBlank),
            occupation = s.occupation.takeIf(String::isNotBlank),
            bloodGroup = s.bloodGroup.takeIf(String::isNotBlank),
            referredBy = s.referredBy.takeIf(String::isNotBlank),
            currentMedication = s.currentMedication.takeIf(String::isNotBlank),
            preferredLanguage = s.language.wire,
        )

        val result = if (s.isNew) {
            repo.createPatient(body)
        } else {
            repo.updatePatient(s.id!!, body)
        }

        val saved = result.getOrElse { e ->
            _state.update { it.copy(saving = false, error = e.message) }
            return null
        }

        // Reception's normal path: register, then put the patient in the queue.
        var queued = false
        if (s.isNew && s.queueAfterSave) {
            when (val outcome = repo.startVisit(saved.id, s.vitals)) {
                is WriteOutcome.Failed -> {
                    _state.update { it.copy(saving = false, error = outcome.message) }
                    return null
                }
                WriteOutcome.Queued -> queued = true
                is WriteOutcome.Synced -> Unit
            }
        }

        _state.update { it.copy(saving = false, id = saved.id, queuedOffline = queued) }
        return saved.id
    }

    companion object {
        const val ERROR_REQUIRED = "required"
        const val ERROR_PHONE = "phone"
        const val ERROR_AGE = "age"
    }
}
