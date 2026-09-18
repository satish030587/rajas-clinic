package com.rajashomoeocare.clinic.data

import com.rajashomoeocare.clinic.data.remote.ApiService
import com.rajashomoeocare.clinic.data.remote.AppointmentCreate
import com.rajashomoeocare.clinic.data.remote.ClinicalUpdate
import com.rajashomoeocare.clinic.data.remote.MessageLogCreate
import com.rajashomoeocare.clinic.data.remote.PatientCreate
import com.rajashomoeocare.clinic.data.remote.QuickAddRequest
import com.rajashomoeocare.clinic.data.remote.StartVisitRequest
import com.rajashomoeocare.clinic.data.remote.TemplateUpdate
import com.rajashomoeocare.clinic.data.remote.UserPatch
import com.rajashomoeocare.clinic.domain.Appointment
import com.rajashomoeocare.clinic.domain.Billing
import com.rajashomoeocare.clinic.domain.Card
import com.rajashomoeocare.clinic.domain.Language
import com.rajashomoeocare.clinic.domain.Medicine
import com.rajashomoeocare.clinic.domain.Patient
import com.rajashomoeocare.clinic.domain.PatientRow
import com.rajashomoeocare.clinic.domain.TemplateKey
import com.rajashomoeocare.clinic.domain.TodaySummary
import com.rajashomoeocare.clinic.domain.Visit
import com.rajashomoeocare.clinic.domain.Vitals
import com.rajashomoeocare.clinic.domain.toDomain
import com.rajashomoeocare.clinic.domain.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

/**
 * The server is the source of truth (spec v3 §3) — two devices cannot share a
 * patient record through a local database.
 *
 * Every call returns a Result so screens can show a real failure instead of an
 * empty list, which on a recall screen would silently read as "nobody is due".
 */
class ClinicRepository(private val api: ApiService) {

    suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: HttpException) {
            Result.failure(ApiError(e.code(), e.friendlyMessage()))
        } catch (e: IOException) {
            Result.failure(ApiError(0, "Cannot reach the clinic server. Check the network."))
        }
    }

    // --- patients ---

    suspend fun patients(query: String? = null): Result<List<PatientRow>> =
        call { api.patients(query?.takeIf { it.isNotBlank() }).map { it.toDomain() } }

    suspend fun patient(id: String): Result<Patient> =
        call { api.patient(id).toDomain() }

    /** Null when the number is free — the duplicate guard (spec §4.11). */
    suspend fun patientByPhone(phone: String): Result<PatientRow?> = call {
        val response = api.patientByPhone(phone)
        if (response.isSuccessful) response.body()?.toDomain() else null
    }

    suspend fun createPatient(body: PatientCreate): Result<Patient> =
        call { api.createPatient(body).toDomain() }

    suspend fun updatePatient(id: String, body: PatientCreate): Result<Patient> =
        call { api.updatePatient(id, body).toDomain() }

    suspend fun quickAddPatient(
        name: String,
        phone: String,
        language: Language,
        lastVisit: LocalDate?,
        nextDue: LocalDate?,
    ): Result<Patient> = call {
        api.quickAddPatient(
            QuickAddRequest(
                name = name,
                phone = phone,
                preferredLanguage = language.wire,
                lastVisitDate = lastVisit?.toString(),
                nextVisitDue = nextDue?.toString(),
            )
        ).toDomain()
    }

    // --- visits ---

    suspend fun queue(): Result<List<Visit>> =
        call { api.queue().map { it.toDomain() } }

    suspend fun visitsForPatient(patientId: String): Result<List<Visit>> =
        call { api.visitsForPatient(patientId).map { it.toDomain() } }

    suspend fun visit(id: String): Result<Visit> = call { api.visit(id).toDomain() }

    suspend fun startVisit(patientId: String, vitals: Vitals?): Result<Visit> = call {
        api.startVisit(
            StartVisitRequest(patientId, vitals?.takeIf { !it.isEmpty }?.toDto())
        ).toDomain()
    }

    suspend fun setVitals(visitId: String, vitals: Vitals): Result<Visit> =
        call { api.setVitals(visitId, vitals.toDto()).toDomain() }

    suspend fun openVisit(id: String): Result<Visit> =
        call { api.openVisit(id).toDomain() }

    suspend fun saveClinical(
        visitId: String,
        complaint: String?,
        cardId: String?,
        nextVisitDue: LocalDate?,
        medicines: List<Medicine>,
        billing: Billing?,
        complete: Boolean,
    ): Result<Visit> = call {
        api.updateClinical(
            visitId,
            ClinicalUpdate(
                complaint = complaint,
                cardId = cardId,
                nextVisitDue = nextVisitDue?.toString(),
                medicines = medicines.map { it.toDto() },
                invoice = billing?.toDto(),
                complete = complete,
            ),
        ).toDomain()
    }

    // --- recall ---

    suspend fun today(): Result<TodaySummary> = call {
        val dto = api.today()
        TodaySummary(
            dueToday = dto.dueToday.map { it.toDomain() },
            overdue = dto.overdue.map { it.toDomain() },
            seenToday = dto.seenToday.map { it.toDomain() },
            bookedToday = dto.bookedToday.map { it.toDomain() },
            collection = dto.collection,
            outstanding = dto.outstanding,
        )
    }

    suspend fun overdue() = call { api.overdue().map { it.toDomain() } }

    suspend fun remindersDue(): Result<List<Appointment>> =
        call { api.remindersDue().map { it.toDomain() } }

    suspend fun bookAppointment(
        patientId: String,
        date: LocalDate,
        note: String? = null,
    ): Result<Appointment> = call {
        api.bookAppointment(
            AppointmentCreate(patientId, date.toString(), note)
        ).toDomain()
    }

    // --- catalog ---

    suspend fun cards(): Result<List<Card>> = call { api.cards().map { it.toDomain() } }

    suspend fun templates(): Result<Map<Pair<TemplateKey, Language>, String>> = call {
        api.templates().associate {
            (TemplateKey.from(it.templateKey) to Language.from(it.language)) to it.bodyText
        }
    }

    suspend fun updateTemplate(key: TemplateKey, language: Language, body: String) =
        call { api.updateTemplate(key.wire, language.wire, TemplateUpdate(body)) }

    suspend fun logMessage(patientId: String, key: TemplateKey, language: Language) =
        call { api.logMessage(MessageLogCreate(patientId, key.wire, language.wire)) }

    suspend fun clinic() = call { api.clinic() }

    suspend fun setUiLanguage(language: Language) =
        call { api.updateMe(UserPatch(uiLanguage = language.wire)) }
}

class ApiError(val code: Int, override val message: String) : Exception(message)

private fun HttpException.friendlyMessage(): String {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val detail = body
        ?.substringAfter("\"detail\":\"", "")
        ?.substringBefore("\"")
        ?.takeIf { it.isNotBlank() }
    return detail ?: when (code()) {
        401 -> "Your session has expired. Please sign in again."
        403 -> "You do not have permission to do that."
        404 -> "Not found."
        409 -> "That record already exists."
        else -> "Something went wrong (${code()})."
    }
}
