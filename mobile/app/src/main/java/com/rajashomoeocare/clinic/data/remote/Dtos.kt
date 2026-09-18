package com.rajashomoeocare.clinic.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Dates and timestamps cross the wire as ISO-8601 strings and are parsed at the
// edges (see toLocalDate()), so the DTOs stay a faithful mirror of the API.

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    val role: String,
    @SerialName("ui_language") val uiLanguage: String,
    val active: Boolean = true,
)

@Serializable
data class UserPatch(
    @SerialName("ui_language") val uiLanguage: String? = null,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class PatientRowDto(
    val id: String,
    val name: String,
    val phone: String,
    val sex: String,
    @SerialName("preferred_language") val preferredLanguage: String,
    @SerialName("current_age") val currentAge: Int? = null,
    @SerialName("last_visit_date") val lastVisitDate: String? = null,
    @SerialName("next_visit_due") val nextVisitDue: String? = null,
    @SerialName("visit_count") val visitCount: Int = 0,
)

@Serializable
data class PatientDto(
    val id: String,
    val name: String,
    val phone: String,
    @SerialName("alternate_phone") val alternatePhone: String? = null,
    val sex: String,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("age_years") val ageYears: Int? = null,
    @SerialName("current_age") val currentAge: Int? = null,
    val address: String? = null,
    val occupation: String? = null,
    @SerialName("blood_group") val bloodGroup: String? = null,
    @SerialName("referred_by") val referredBy: String? = null,
    @SerialName("current_medication") val currentMedication: String? = null,
    @SerialName("preferred_language") val preferredLanguage: String,
    @SerialName("migrated_from_myopd") val migratedFromMyopd: Boolean = false,
    val archived: Boolean = false,
)

@Serializable
data class PatientCreate(
    val name: String,
    val phone: String,
    val sex: String,
    @SerialName("alternate_phone") val alternatePhone: String? = null,
    @SerialName("age_years") val ageYears: Int? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val address: String? = null,
    val occupation: String? = null,
    @SerialName("blood_group") val bloodGroup: String? = null,
    @SerialName("referred_by") val referredBy: String? = null,
    @SerialName("current_medication") val currentMedication: String? = null,
    @SerialName("preferred_language") val preferredLanguage: String = "en",
)

@Serializable
data class QuickAddRequest(
    val name: String,
    val phone: String,
    @SerialName("preferred_language") val preferredLanguage: String = "en",
    @SerialName("last_visit_date") val lastVisitDate: String? = null,
    @SerialName("next_visit_due") val nextVisitDue: String? = null,
)

@Serializable
data class VitalsDto(
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("bp_systolic") val bpSystolic: Int? = null,
    @SerialName("bp_diastolic") val bpDiastolic: Int? = null,
    val pulse: Int? = null,
)

@Serializable
data class MedicineDto(
    val id: String? = null,
    @SerialName("medicine_name") val medicineName: String,
    val potency: String? = null,
    val form: String = "pills",
    val quantity: String? = null,
)

@Serializable
data class InvoiceDto(
    @SerialName("consultation_fee") val consultationFee: Int = 0,
    @SerialName("medicine_charge") val medicineCharge: Int = 0,
    @SerialName("payment_mode") val paymentMode: String = "cash",
    val paid: Boolean = true,
    val total: Int = 0,
)

@Serializable
data class VisitDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("visit_date") val visitDate: String,
    val status: String,
    val source: String = "recorded",
    val complaint: String? = null,
    @SerialName("card_id") val cardId: String? = null,
    @SerialName("next_visit_due") val nextVisitDue: String? = null,
    val vitals: VitalsDto? = null,
    val medicines: List<MedicineDto> = emptyList(),
    val invoice: InvoiceDto? = null,
)

@Serializable
data class StartVisitRequest(
    @SerialName("patient_id") val patientId: String,
    val vitals: VitalsDto? = null,
)

@Serializable
data class ClinicalUpdate(
    val complaint: String? = null,
    @SerialName("card_id") val cardId: String? = null,
    @SerialName("next_visit_due") val nextVisitDue: String? = null,
    val medicines: List<MedicineDto>? = null,
    val invoice: InvoiceDto? = null,
    val complete: Boolean = false,
)

@Serializable
data class CardDto(
    val id: String,
    val code: String,
    @SerialName("label_en") val labelEn: String,
    @SerialName("label_ta") val labelTa: String,
    @SerialName("body_en") val bodyEn: String,
    @SerialName("body_ta") val bodyTa: String,
)

@Serializable
data class RecallItemDto(
    val patient: PatientRowDto,
    @SerialName("due_date") val dueDate: String,
    @SerialName("days_overdue") val daysOverdue: Int,
)

@Serializable
data class AppointmentDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("appointment_date") val appointmentDate: String,
    val note: String? = null,
    val status: String,
)

@Serializable
data class AppointmentCreate(
    @SerialName("patient_id") val patientId: String,
    @SerialName("appointment_date") val appointmentDate: String,
    val note: String? = null,
)

@Serializable
data class TodaySummaryDto(
    @SerialName("due_today") val dueToday: List<RecallItemDto> = emptyList(),
    val overdue: List<RecallItemDto> = emptyList(),
    @SerialName("seen_today") val seenToday: List<PatientRowDto> = emptyList(),
    @SerialName("booked_today") val bookedToday: List<AppointmentDto> = emptyList(),
    val collection: Int = 0,
    val outstanding: Int = 0,
)

@Serializable
data class TemplateDto(
    @SerialName("template_key") val templateKey: String,
    val language: String,
    @SerialName("body_text") val bodyText: String,
)

@Serializable
data class TemplateUpdate(@SerialName("body_text") val bodyText: String)

@Serializable
data class MessageLogCreate(
    @SerialName("patient_id") val patientId: String,
    @SerialName("template_key") val templateKey: String,
    val language: String,
)

@Serializable
data class InvestigationFileDto(
    val id: String,
    @SerialName("file_path") val filePath: String,
    @SerialName("page_no") val pageNo: Int = 1,
)

@Serializable
data class InvestigationDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("visit_id") val visitId: String? = null,
    val kind: String = "scan",
    val title: String,
    @SerialName("taken_on") val takenOn: String,
    val note: String? = null,
    val files: List<InvestigationFileDto> = emptyList(),
)

@Serializable
data class InvestigationCreate(
    @SerialName("patient_id") val patientId: String,
    @SerialName("visit_id") val visitId: String? = null,
    val kind: String = "scan",
    val title: String,
    @SerialName("taken_on") val takenOn: String,
    val note: String? = null,
)

@Serializable
data class ClinicProfileDto(
    val name: String = "",
    @SerialName("doctor_name") val doctorName: String = "",
    @SerialName("doctor_qualifications") val doctorQualifications: String = "",
    val phone: String = "",
    val address: String = "",
    val landmark: String = "",
    @SerialName("map_link") val mapLink: String = "",
    @SerialName("working_hours") val workingHours: String = "",
    @SerialName("review_link") val reviewLink: String = "",
    @SerialName("upi_id") val upiId: String = "",
    @SerialName("default_consultation_fee") val defaultConsultationFee: Int = 200,
)
