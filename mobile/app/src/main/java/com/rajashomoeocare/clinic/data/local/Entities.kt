package com.rajashomoeocare.clinic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

enum class Sex { MALE, FEMALE, OTHER }

enum class Language { EN, TA }

enum class PaymentMode { CASH, UPI }

enum class TemplateKey { WELCOME, RECALL }

enum class MessageChannel { WHATSAPP_MANUAL, WHATSAPP_API, SMS }

@Entity(
    tableName = "patients",
    indices = [Index("name"), Index("phone")],
)
data class PatientEntity(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val phone: String,
    val alternatePhone: String? = null,
    val sex: Sex,
    val dateOfBirth: LocalDate? = null,
    val ageYears: Int? = null,
    val ageRecordedOn: LocalDate? = null,
    val address: String? = null,
    val occupation: String? = null,
    val bloodGroup: String? = null,
    val referredBy: String? = null,
    val currentMedication: String? = null,
    val preferredLanguage: Language = Language.EN,
    val createdAt: Instant = Instant.now(),
    val archived: Boolean = false,
)

@Entity(
    tableName = "visits",
    indices = [Index("patientId"), Index("visitDate"), Index("nextVisitDue")],
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class VisitEntity(
    @PrimaryKey val id: String = newId(),
    val patientId: String,
    val visitDate: LocalDate,
    val complaint: String? = null,
    val remedyGiven: String? = null,
    val potency: String? = null,
    val adviceGiven: String? = null,
    /** Spec §3: the single field the whole recall system runs on. */
    val nextVisitDue: LocalDate? = null,
    val createdAt: Instant = Instant.now(),
)

@Entity(
    tableName = "visit_photos",
    indices = [Index("visitId")],
    foreignKeys = [
        ForeignKey(
            entity = VisitEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PhotoEntity(
    @PrimaryKey val id: String = newId(),
    val visitId: String,
    val filePath: String,
    val caption: String? = null,
    val takenAt: Instant = Instant.now(),
)

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["visitId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = VisitEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class InvoiceEntity(
    @PrimaryKey val id: String = newId(),
    val visitId: String,
    val consultationFee: Int = 0,
    val medicineCharge: Int = 0,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val paid: Boolean = true,
    val paidAt: Instant? = null,
) {
    val total: Int get() = consultationFee + medicineCharge
}

@Entity(
    tableName = "message_templates",
    primaryKeys = ["templateKey", "language"],
)
data class MessageTemplateEntity(
    val templateKey: TemplateKey,
    val language: Language,
    val bodyText: String,
    val updatedAt: Instant = Instant.now(),
)

@Entity(
    tableName = "message_log",
    indices = [Index("patientId"), Index("sentAt")],
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MessageLogEntity(
    @PrimaryKey val id: String = newId(),
    val patientId: String,
    val templateKey: TemplateKey,
    val language: Language,
    val channel: MessageChannel = MessageChannel.WHATSAPP_MANUAL,
    val sentAt: Instant = Instant.now(),
)
