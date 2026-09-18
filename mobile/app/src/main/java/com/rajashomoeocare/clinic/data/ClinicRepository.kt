package com.rajashomoeocare.clinic.data

import com.rajashomoeocare.clinic.BuildConfig
import com.rajashomoeocare.clinic.data.remote.ApiService
import com.rajashomoeocare.clinic.data.remote.AppointmentCreate
import com.rajashomoeocare.clinic.data.remote.ClinicProfileDto
import com.rajashomoeocare.clinic.data.remote.ClinicalUpdate
import com.rajashomoeocare.clinic.data.remote.InvestigationCreate
import com.rajashomoeocare.clinic.data.remote.MessageLogCreate
import com.rajashomoeocare.clinic.data.remote.PatientCreate
import com.rajashomoeocare.clinic.data.remote.QuickAddRequest
import com.rajashomoeocare.clinic.data.remote.StartVisitRequest
import com.rajashomoeocare.clinic.data.remote.TemplateUpdate
import com.rajashomoeocare.clinic.data.remote.UserPatch
import com.rajashomoeocare.clinic.domain.Appointment
import com.rajashomoeocare.clinic.domain.Billing
import com.rajashomoeocare.clinic.domain.Card
import com.rajashomoeocare.clinic.domain.Investigation
import com.rajashomoeocare.clinic.domain.InvestigationKind
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.time.LocalDate

/**
 * The server is the source of truth (spec v3 §3) — two devices cannot share a
 * patient record through a local database.
 *
 * Every call returns a Result so screens can show a real failure instead of an
 * empty list, which on a recall screen would silently read as "nobody is due".
 */
/**
 * What happened to a write. Queued is reported honestly rather than as success:
 * a clinical record that has not reached the server must not look as if it has.
 */
sealed interface WriteOutcome {
    data class Synced(val visit: Visit) : WriteOutcome
    data object Queued : WriteOutcome
    data class Failed(val message: String) : WriteOutcome
}

class ClinicRepository(
    private val api: ApiService,
    private val outbox: OutboxStore,
    private val sync: OutboxSync,
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val pendingWrites = outbox.pending

    /** Opportunistic: any successful call is proof the network is back. */
    suspend fun drainOutbox(): Int = runCatching { sync.drain() }.getOrDefault(0)

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

    suspend fun startVisit(patientId: String, vitals: Vitals?): WriteOutcome {
        val body = StartVisitRequest(patientId, vitals?.takeIf { !it.isEmpty }?.toDto())
        return queueable(
            request = { api.startVisit(body).toDomain() },
            kind = PendingWrite.Kind.START_VISIT,
            payload = json.encodeToString(body),
        )
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
    ): WriteOutcome {
        val body = ClinicalUpdate(
            complaint = complaint,
            cardId = cardId,
            nextVisitDue = nextVisitDue?.toString(),
            medicines = medicines.map { it.toDto() },
            invoice = billing?.toDto(),
            complete = complete,
        )
        return queueable(
            request = { api.updateClinical(visitId, body).toDomain() },
            kind = PendingWrite.Kind.SAVE_CLINICAL,
            payload = json.encodeToString(body),
            targetId = visitId,
        )
    }

    /**
     * Sends now, or queues if the network is down. Used only for writes that
     * must not block clinic work; reads simply fail and show a retry.
     */
    private suspend fun queueable(
        request: suspend () -> Visit,
        kind: PendingWrite.Kind,
        payload: String,
        targetId: String = "",
    ): WriteOutcome = withContext(Dispatchers.IO) {
        try {
            val visit = request()
            sync.drain()
            WriteOutcome.Synced(visit)
        } catch (e: IOException) {
            outbox.add(PendingWrite(kind = kind, payload = payload, targetId = targetId))
            WriteOutcome.Queued
        } catch (e: HttpException) {
            WriteOutcome.Failed(e.friendlyMessage())
        }
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

    // --- investigations ---

    suspend fun investigations(patientId: String): Result<List<Investigation>> =
        call { api.investigations(patientId).map { it.toDomain() } }

    suspend fun createInvestigation(
        patientId: String,
        title: String,
        kind: InvestigationKind,
        takenOn: LocalDate,
        note: String?,
    ): Result<Investigation> = call {
        api.createInvestigation(
            InvestigationCreate(
                patientId = patientId,
                title = title.trim(),
                kind = kind.wire,
                takenOn = takenOn.toString(),
                note = note?.takeIf { it.isNotBlank() },
            )
        ).toDomain()
    }

    suspend fun uploadInvestigationFile(
        investigationId: String,
        file: File,
        mimeType: String,
    ): Result<Investigation> = call {
        val part = MultipartBody.Part.createFormData(
            "file", file.name, file.asRequestBody(mimeType.toMediaType()),
        )
        api.uploadInvestigationFile(investigationId, part).toDomain()
    }

    suspend fun deleteInvestigation(id: String) = call { api.deleteInvestigation(id) }

    /** Reports are fetched with the same auth as everything else (see ApiClient). */
    fun fileUrl(fileId: String): String =
        BuildConfig.API_BASE_URL + "investigations/files/$fileId"

    // --- catalog ---

    suspend fun cards(): Result<List<Card>> = call { api.cards().map { it.toDomain() } }

    suspend fun templates(): Result<Map<Pair<TemplateKey, Language>, String>> = call {
        api.templates().associate {
            (TemplateKey.from(it.templateKey) to Language.from(it.language)) to it.bodyText
        }
    }

    suspend fun updateTemplate(key: TemplateKey, language: Language, body: String) =
        call { api.updateTemplate(key.wire, language.wire, TemplateUpdate(body)) }

    suspend fun logMessage(patientId: String, key: TemplateKey, language: Language) {
        val body = MessageLogCreate(patientId, key.wire, language.wire)
        withContext(Dispatchers.IO) {
            try {
                api.logMessage(body)
            } catch (_: IOException) {
                // The message itself already went out via WhatsApp; only the
                // record of it is delayed, and losing that breaks the Phase 2
                // recall-conversion report.
                outbox.add(
                    PendingWrite(
                        kind = PendingWrite.Kind.LOG_MESSAGE,
                        payload = json.encodeToString(body),
                    )
                )
            } catch (_: HttpException) {
                // Nothing useful to do; the send still happened.
            }
        }
    }

    suspend fun clinic() = call { api.clinic() }

    suspend fun updateClinic(profile: ClinicProfileDto) =
        call { api.updateClinic(profile) }

    suspend fun appointmentsFor(patientId: String): Result<List<Appointment>> =
        call { api.appointments(patientId = patientId).map { it.toDomain() } }

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
