package com.rajashomoeocare.clinic.domain

import com.rajashomoeocare.clinic.data.remote.AppointmentDto
import com.rajashomoeocare.clinic.data.remote.CardDto
import com.rajashomoeocare.clinic.data.remote.InvestigationDto
import com.rajashomoeocare.clinic.data.remote.InvoiceDto
import com.rajashomoeocare.clinic.data.remote.MedicineDto
import com.rajashomoeocare.clinic.data.remote.PatientDto
import com.rajashomoeocare.clinic.data.remote.PatientRowDto
import com.rajashomoeocare.clinic.data.remote.RecallItemDto
import com.rajashomoeocare.clinic.data.remote.VisitDto
import com.rajashomoeocare.clinic.data.remote.VitalsDto
import java.time.LocalDate

enum class Sex { MALE, FEMALE, OTHER;
    val wire: String get() = name.lowercase()
    companion object {
        fun from(v: String?) = when (v?.lowercase()) {
            "male" -> MALE; "female" -> FEMALE; else -> OTHER
        }
    }
}

enum class Language { EN, TA;
    val wire: String get() = name.lowercase()
    companion object {
        fun from(v: String?) = if (v?.lowercase() == "ta") TA else EN
    }
}

enum class PaymentMode { CASH, UPI;
    val wire: String get() = name.lowercase()
    companion object {
        fun from(v: String?) = if (v?.lowercase() == "upi") UPI else CASH
    }
}

enum class MedicineForm { PILLS, DROPS;
    val wire: String get() = name.lowercase()
    companion object {
        fun from(v: String?) = if (v?.lowercase() == "drops") DROPS else PILLS
    }
}

enum class VisitStatus { WAITING, IN_CONSULTATION, COMPLETED;
    companion object {
        fun from(v: String?) = when (v?.lowercase()) {
            "waiting" -> WAITING
            "in_consultation" -> IN_CONSULTATION
            else -> COMPLETED
        }
    }
}

enum class TemplateKey(val wire: String) {
    WELCOME("welcome"),
    RECALL("recall"),
    APPOINTMENT_CONFIRMATION("appointment_confirmation"),
    APPOINTMENT_REMINDER("appointment_reminder"),
    MEDICINE_DISPATCHED("medicine_dispatched"),
    REVIEW_REQUEST("review_request");

    companion object {
        fun from(v: String?) = entries.firstOrNull { it.wire == v } ?: WELCOME
    }
}

private fun String?.toDate(): LocalDate? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

data class PatientRow(
    val id: String,
    val name: String,
    val phone: String,
    val sex: Sex,
    val age: Int?,
    val preferredLanguage: Language,
    val lastVisitDate: LocalDate?,
    val nextVisitDue: LocalDate?,
    val visitCount: Int,
)

data class Patient(
    val id: String,
    val name: String,
    val phone: String,
    val alternatePhone: String?,
    val sex: Sex,
    val age: Int?,
    val ageYears: Int?,
    val dateOfBirth: LocalDate?,
    val address: String?,
    val occupation: String?,
    val bloodGroup: String?,
    val referredBy: String?,
    val currentMedication: String?,
    val preferredLanguage: Language,
    val migratedFromMyopd: Boolean,
)

data class Vitals(
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bpSystolic: Int? = null,
    val bpDiastolic: Int? = null,
    val pulse: Int? = null,
) {
    val isEmpty: Boolean
        get() = heightCm == null && weightKg == null &&
            bpSystolic == null && bpDiastolic == null && pulse == null

    val bloodPressure: String?
        get() = if (bpSystolic != null && bpDiastolic != null) {
            "$bpSystolic/$bpDiastolic"
        } else null
}

data class Medicine(
    val name: String,
    val potency: String? = null,
    val form: MedicineForm = MedicineForm.PILLS,
    val quantity: String? = null,
)

data class Billing(
    val consultationFee: Int = 0,
    val medicineCharge: Int = 0,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val paid: Boolean = true,
) {
    val total: Int get() = consultationFee + medicineCharge
}

data class Visit(
    val id: String,
    val patientId: String,
    val visitDate: LocalDate,
    val status: VisitStatus,
    val migrated: Boolean,
    val complaint: String?,
    val cardId: String?,
    val nextVisitDue: LocalDate?,
    val vitals: Vitals?,
    val medicines: List<Medicine>,
    val billing: Billing?,
)

data class Card(
    val id: String,
    val code: String,
    val labelEn: String,
    val labelTa: String,
    val bodyEn: String,
    val bodyTa: String,
) {
    fun label(language: Language) = if (language == Language.TA) labelTa else labelEn
    fun body(language: Language) = if (language == Language.TA) bodyTa else bodyEn
}

enum class InvestigationKind(val wire: String) {
    SCAN("scan"), LAB("lab"), XRAY("xray"), ECG("ecg"), OTHER("other");

    companion object {
        fun from(v: String?) = entries.firstOrNull { it.wire == v } ?: SCAN
    }
}

data class InvestigationFile(val id: String, val pageNo: Int)

/**
 * An outside report the patient brought in. Grouped by [title] so the same
 * study across months can be compared side by side (spec §4.5).
 */
data class Investigation(
    val id: String,
    val patientId: String,
    val kind: InvestigationKind,
    val title: String,
    val takenOn: LocalDate,
    val note: String?,
    val files: List<InvestigationFile>,
)

data class Appointment(
    val id: String,
    val patientId: String,
    val date: LocalDate,
    val note: String?,
    val status: String,
)

data class TodaySummary(
    val dueToday: List<RecallItem> = emptyList(),
    val overdue: List<RecallItem> = emptyList(),
    val seenToday: List<PatientRow> = emptyList(),
    val bookedToday: List<Appointment> = emptyList(),
    val collection: Int = 0,
    val outstanding: Int = 0,
)

// --- mappers ---

fun PatientRowDto.toDomain() = PatientRow(
    id = id,
    name = name,
    phone = phone,
    sex = Sex.from(sex),
    age = currentAge,
    preferredLanguage = Language.from(preferredLanguage),
    lastVisitDate = lastVisitDate.toDate(),
    nextVisitDue = nextVisitDue.toDate(),
    visitCount = visitCount,
)

fun PatientDto.toDomain() = Patient(
    id = id,
    name = name,
    phone = phone,
    alternatePhone = alternatePhone,
    sex = Sex.from(sex),
    age = currentAge,
    ageYears = ageYears,
    dateOfBirth = dateOfBirth.toDate(),
    address = address,
    occupation = occupation,
    bloodGroup = bloodGroup,
    referredBy = referredBy,
    currentMedication = currentMedication,
    preferredLanguage = Language.from(preferredLanguage),
    migratedFromMyopd = migratedFromMyopd,
)

fun VitalsDto.toDomain() = Vitals(heightCm, weightKg, bpSystolic, bpDiastolic, pulse)

fun Vitals.toDto() = VitalsDto(heightCm, weightKg, bpSystolic, bpDiastolic, pulse)

fun MedicineDto.toDomain() =
    Medicine(medicineName, potency, MedicineForm.from(form), quantity)

fun Medicine.toDto() = MedicineDto(
    medicineName = name, potency = potency, form = form.wire, quantity = quantity,
)

fun InvoiceDto.toDomain() = Billing(
    consultationFee, medicineCharge, PaymentMode.from(paymentMode), paid,
)

fun Billing.toDto() = InvoiceDto(
    consultationFee = consultationFee,
    medicineCharge = medicineCharge,
    paymentMode = paymentMode.wire,
    paid = paid,
)

fun VisitDto.toDomain() = Visit(
    id = id,
    patientId = patientId,
    visitDate = visitDate.toDate() ?: LocalDate.now(),
    status = VisitStatus.from(status),
    migrated = source == "migrated",
    complaint = complaint,
    cardId = cardId,
    nextVisitDue = nextVisitDue.toDate(),
    vitals = vitals?.toDomain(),
    medicines = medicines.map { it.toDomain() },
    billing = invoice?.toDomain(),
)

fun CardDto.toDomain() = Card(id, code, labelEn, labelTa, bodyEn, bodyTa)

fun InvestigationDto.toDomain() = Investigation(
    id = id,
    patientId = patientId,
    kind = InvestigationKind.from(kind),
    title = title,
    takenOn = takenOn.toDate() ?: LocalDate.now(),
    note = note,
    files = files.map { InvestigationFile(it.id, it.pageNo) },
)

fun AppointmentDto.toDomain() = Appointment(
    id = id,
    patientId = patientId,
    date = appointmentDate.toDate() ?: LocalDate.now(),
    note = note,
    status = status,
)

fun RecallItemDto.toDomain() = RecallItem(
    patient = patient.toDomain(),
    dueDate = dueDate.toDate() ?: LocalDate.now(),
    daysOverdue = daysOverdue,
)
