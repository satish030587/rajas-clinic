package com.rajashomoeocare.clinic.data

import com.rajashomoeocare.clinic.data.local.ClinicDatabase
import com.rajashomoeocare.clinic.data.local.InvoiceEntity
import com.rajashomoeocare.clinic.data.local.Language
import com.rajashomoeocare.clinic.data.local.MessageChannel
import com.rajashomoeocare.clinic.data.local.MessageLogEntity
import com.rajashomoeocare.clinic.data.local.MessageTemplateEntity
import com.rajashomoeocare.clinic.data.local.PatientEntity
import com.rajashomoeocare.clinic.data.local.PhotoEntity
import com.rajashomoeocare.clinic.data.local.TemplateKey
import com.rajashomoeocare.clinic.data.local.VisitEntity
import com.rajashomoeocare.clinic.domain.defaultTemplates
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

class ClinicRepository(private val db: ClinicDatabase) {

    val patients = db.patients()
    val visits = db.visits()
    val photos = db.photos()
    val invoices = db.invoices()
    val templates = db.templates()
    val messageLog = db.messageLog()

    suspend fun ensureTemplatesSeeded() {
        if (templates.count() == 0) {
            defaultTemplates().forEach { templates.upsert(it) }
        }
    }

    suspend fun savePatient(patient: PatientEntity, isNew: Boolean) {
        if (isNew) patients.insert(patient) else patients.update(patient)
    }

    /**
     * A visit, its bill and any photos taken during it are one action from the
     * doctor's point of view, so they are written together — and nothing is written
     * at all until he saves, so an abandoned entry leaves no row behind.
     */
    suspend fun saveVisit(
        visit: VisitEntity,
        invoice: InvoiceEntity?,
        newPhotoPaths: List<String>,
    ) {
        visits.upsert(visit)
        invoice?.let { invoices.upsert(it.copy(visitId = visit.id)) }
        newPhotoPaths.forEach { path ->
            photos.insert(PhotoEntity(visitId = visit.id, filePath = path))
        }
    }

    suspend fun template(key: TemplateKey, language: Language): MessageTemplateEntity? =
        templates.get(key, language)

    suspend fun recordMessageSent(patientId: String, key: TemplateKey, language: Language) {
        messageLog.insert(
            MessageLogEntity(
                patientId = patientId,
                templateKey = key,
                language = language,
                channel = MessageChannel.WHATSAPP_MANUAL,
                sentAt = Instant.now(),
            )
        )
    }

    fun collectionOn(day: LocalDate): Flow<Int> = invoices.observeCollectionOn(day)

    fun outstandingOn(day: LocalDate): Flow<Int> = invoices.observeOutstandingOn(day)
}
