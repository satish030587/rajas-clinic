package com.rajashomoeocare.clinic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajashomoeocare.clinic.data.ClinicRepository
import com.rajashomoeocare.clinic.data.local.Language
import com.rajashomoeocare.clinic.data.local.PatientEntity
import com.rajashomoeocare.clinic.data.local.Sex
import com.rajashomoeocare.clinic.util.normalizePhone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class FormField { NAME, PHONE, ALT_PHONE, AGE }

data class PatientFormState(
    val id: String? = null,
    val name: String = "",
    val phone: String = "",
    val alternatePhone: String = "",
    val sex: Sex = Sex.MALE,
    val ageText: String = "",
    val dateOfBirth: LocalDate? = null,
    val address: String = "",
    val occupation: String = "",
    val bloodGroup: String = "",
    val referredBy: String = "",
    val currentMedication: String = "",
    val language: Language = Language.EN,
    val errors: Map<FormField, String> = emptyMap(),
    val saving: Boolean = false,
) {
    val isNew: Boolean get() = id == null
}

class PatientFormViewModel(
    private val repo: ClinicRepository,
    private val patientId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientFormState())
    val state: StateFlow<PatientFormState> = _state.asStateFlow()

    private var loaded: PatientEntity? = null

    init {
        if (patientId != null) {
            viewModelScope.launch {
                repo.patients.get(patientId)?.let { p ->
                    loaded = p
                    _state.value = PatientFormState(
                        id = p.id,
                        name = p.name,
                        phone = p.phone,
                        alternatePhone = p.alternatePhone.orEmpty(),
                        sex = p.sex,
                        ageText = p.ageYears?.toString().orEmpty(),
                        dateOfBirth = p.dateOfBirth,
                        address = p.address.orEmpty(),
                        occupation = p.occupation.orEmpty(),
                        bloodGroup = p.bloodGroup.orEmpty(),
                        referredBy = p.referredBy.orEmpty(),
                        currentMedication = p.currentMedication.orEmpty(),
                        language = p.preferredLanguage,
                    )
                }
            }
        }
    }

    fun edit(transform: (PatientFormState) -> PatientFormState) {
        _state.update { transform(it).copy(errors = emptyMap()) }
    }

    /** Returns the saved patient id, or null when validation failed. */
    suspend fun save(): String? {
        val s = _state.value
        val errors = buildMap {
            if (s.name.isBlank()) put(FormField.NAME, ERROR_REQUIRED)
            if (normalizePhone(s.phone) == null) put(FormField.PHONE, ERROR_PHONE)
            if (s.alternatePhone.isNotBlank() && normalizePhone(s.alternatePhone) == null) {
                put(FormField.ALT_PHONE, ERROR_PHONE)
            }
            val age = s.ageText.toIntOrNull()
            if (s.dateOfBirth == null && (age == null || age !in 0..120)) {
                put(FormField.AGE, ERROR_AGE)
            }
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return null
        }

        _state.update { it.copy(saving = true) }
        val age = s.ageText.toIntOrNull()
        val base = loaded ?: PatientEntity(
            name = "", phone = "", sex = Sex.MALE,
        )
        val entity = base.copy(
            name = s.name.trim().split(Regex("\\s+")).joinToString(" "),
            phone = normalizePhone(s.phone)!!,
            alternatePhone = s.alternatePhone.takeIf(String::isNotBlank)?.let(::normalizePhone),
            sex = s.sex,
            dateOfBirth = s.dateOfBirth,
            ageYears = if (s.dateOfBirth == null) age else null,
            ageRecordedOn = if (s.dateOfBirth == null && age != null) LocalDate.now() else null,
            address = s.address.takeIf(String::isNotBlank),
            occupation = s.occupation.takeIf(String::isNotBlank),
            bloodGroup = s.bloodGroup.takeIf(String::isNotBlank),
            referredBy = s.referredBy.takeIf(String::isNotBlank),
            currentMedication = s.currentMedication.takeIf(String::isNotBlank),
            preferredLanguage = s.language,
        )
        repo.savePatient(entity, isNew = s.isNew)
        _state.update { it.copy(saving = false, id = entity.id) }
        return entity.id
    }

    companion object {
        const val ERROR_REQUIRED = "required"
        const val ERROR_PHONE = "phone"
        const val ERROR_AGE = "age"
    }
}
