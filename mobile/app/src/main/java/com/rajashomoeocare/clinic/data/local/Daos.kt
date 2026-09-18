package com.rajashomoeocare.clinic.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** A patient joined with their most recent visit — what every list row needs. */
data class PatientRow(
    val id: String,
    val name: String,
    val phone: String,
    val sex: Sex,
    val dateOfBirth: LocalDate?,
    val ageYears: Int?,
    val ageRecordedOn: LocalDate?,
    val preferredLanguage: Language,
    val lastVisitDate: LocalDate?,
    val nextVisitDue: LocalDate?,
    val visitCount: Int,
)

data class VisitWithBilling(
    val id: String,
    val patientId: String,
    val visitDate: LocalDate,
    val complaint: String?,
    val remedyGiven: String?,
    val potency: String?,
    val adviceGiven: String?,
    val nextVisitDue: LocalDate?,
    val consultationFee: Int?,
    val medicineCharge: Int?,
    val paymentMode: PaymentMode?,
    val paid: Boolean?,
    val photoCount: Int,
)

/**
 * The latest visit per patient, as a reusable sub-select. A patient is "due" when
 * the next-visit date they left with has arrived and they have not been back since.
 */
private const val LATEST_VISIT = """
    SELECT v.* FROM visits v
    WHERE v.id = (
        SELECT id FROM visits
        WHERE patientId = v.patientId
        ORDER BY visitDate DESC, createdAt DESC
        LIMIT 1
    )
"""

@Dao
interface PatientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: PatientEntity)

    @Update
    suspend fun update(patient: PatientEntity)

    @Query("SELECT * FROM patients WHERE id = :id")
    fun observe(id: String): Flow<PatientEntity?>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun get(id: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE phone = :phone AND archived = 0 LIMIT 1")
    suspend fun findByPhone(phone: String): PatientEntity?

    @Query("UPDATE patients SET archived = 1 WHERE id = :id")
    suspend fun archive(id: String)

    @Query("SELECT COUNT(*) FROM patients WHERE archived = 0")
    fun observeCount(): Flow<Int>

    @Transaction
    @Query(
        """
        SELECT p.id, p.name, p.phone, p.sex, p.dateOfBirth, p.ageYears,
               p.ageRecordedOn, p.preferredLanguage,
               lv.visitDate AS lastVisitDate, lv.nextVisitDue AS nextVisitDue,
               (SELECT COUNT(*) FROM visits WHERE patientId = p.id) AS visitCount
        FROM patients p
        LEFT JOIN ($LATEST_VISIT) lv ON lv.patientId = p.id
        WHERE p.archived = 0
          AND (:query = '' OR p.name LIKE '%' || :query || '%' OR p.phone LIKE '%' || :query || '%')
        ORDER BY p.name COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<PatientRow>>

    /**
     * Everyone whose recall date has arrived or passed, soonest-due first.
     * Drives both the Today list and the overdue buckets.
     */
    @Transaction
    @Query(
        """
        SELECT p.id, p.name, p.phone, p.sex, p.dateOfBirth, p.ageYears,
               p.ageRecordedOn, p.preferredLanguage,
               lv.visitDate AS lastVisitDate, lv.nextVisitDue AS nextVisitDue,
               (SELECT COUNT(*) FROM visits WHERE patientId = p.id) AS visitCount
        FROM patients p
        JOIN ($LATEST_VISIT) lv ON lv.patientId = p.id
        WHERE p.archived = 0
          AND lv.nextVisitDue IS NOT NULL
          AND lv.nextVisitDue <= :today
        ORDER BY lv.nextVisitDue ASC
        """
    )
    fun observeDueOnOrBefore(today: LocalDate): Flow<List<PatientRow>>

    /** Patients scheduled for a future date — the upcoming strip on Today. */
    @Transaction
    @Query(
        """
        SELECT p.id, p.name, p.phone, p.sex, p.dateOfBirth, p.ageYears,
               p.ageRecordedOn, p.preferredLanguage,
               lv.visitDate AS lastVisitDate, lv.nextVisitDue AS nextVisitDue,
               (SELECT COUNT(*) FROM visits WHERE patientId = p.id) AS visitCount
        FROM patients p
        JOIN ($LATEST_VISIT) lv ON lv.patientId = p.id
        WHERE p.archived = 0
          AND lv.nextVisitDue > :today
          AND lv.nextVisitDue <= :until
        ORDER BY lv.nextVisitDue ASC
        """
    )
    fun observeUpcoming(today: LocalDate, until: LocalDate): Flow<List<PatientRow>>
}

@Dao
interface VisitDao {

    // Upsert rather than INSERT OR REPLACE: REPLACE deletes the row first, which
    // would cascade-delete the visit's photos on every edit.
    @Upsert
    suspend fun upsert(visit: VisitEntity)

    @Delete
    suspend fun delete(visit: VisitEntity)

    @Query("SELECT * FROM visits WHERE id = :id")
    suspend fun get(id: String): VisitEntity?

    @Query(
        """
        SELECT v.id, v.patientId, v.visitDate, v.complaint, v.remedyGiven, v.potency,
               v.adviceGiven, v.nextVisitDue,
               i.consultationFee, i.medicineCharge, i.paymentMode, i.paid,
               (SELECT COUNT(*) FROM visit_photos WHERE visitId = v.id) AS photoCount
        FROM visits v
        LEFT JOIN invoices i ON i.visitId = v.id
        WHERE v.patientId = :patientId
        ORDER BY v.visitDate DESC, v.createdAt DESC
        """
    )
    fun observeForPatient(patientId: String): Flow<List<VisitWithBilling>>

    @Query("SELECT COUNT(*) FROM visits WHERE visitDate = :day")
    fun observeCountOn(day: LocalDate): Flow<Int>

    @Query(
        """
        SELECT p.id, p.name, p.phone, p.sex, p.dateOfBirth, p.ageYears,
               p.ageRecordedOn, p.preferredLanguage,
               v.visitDate AS lastVisitDate, v.nextVisitDue AS nextVisitDue,
               (SELECT COUNT(*) FROM visits WHERE patientId = p.id) AS visitCount
        FROM visits v
        JOIN patients p ON p.id = v.patientId
        WHERE v.visitDate = :day
        ORDER BY v.createdAt DESC
        """
    )
    fun observeSeenOn(day: LocalDate): Flow<List<PatientRow>>

    /** Whether the patient has been back since a given date — recall conversion. */
    @Query("SELECT COUNT(*) FROM visits WHERE patientId = :patientId AND visitDate >= :since")
    suspend fun countVisitsSince(patientId: String, since: LocalDate): Int
}

@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity)

    @Delete
    suspend fun delete(photo: PhotoEntity)

    @Query("SELECT * FROM visit_photos WHERE visitId = :visitId ORDER BY takenAt ASC")
    fun observeForVisit(visitId: String): Flow<List<PhotoEntity>>

    /** Every photo for a patient, oldest first — the comparison timeline. */
    @Query(
        """
        SELECT ph.* FROM visit_photos ph
        JOIN visits v ON v.id = ph.visitId
        WHERE v.patientId = :patientId
        ORDER BY ph.takenAt ASC
        """
    )
    fun observeForPatient(patientId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM visit_photos WHERE id = :id")
    suspend fun get(id: String): PhotoEntity?
}

@Dao
interface InvoiceDao {

    @Upsert
    suspend fun upsert(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices WHERE visitId = :visitId")
    suspend fun forVisit(visitId: String): InvoiceEntity?

    @Query(
        """
        SELECT COALESCE(SUM(i.consultationFee + i.medicineCharge), 0)
        FROM invoices i
        JOIN visits v ON v.id = i.visitId
        WHERE v.visitDate = :day AND i.paid = 1
        """
    )
    fun observeCollectionOn(day: LocalDate): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(i.consultationFee + i.medicineCharge), 0)
        FROM invoices i
        JOIN visits v ON v.id = i.visitId
        WHERE v.visitDate = :day AND i.paid = 0
        """
    )
    fun observeOutstandingOn(day: LocalDate): Flow<Int>
}

@Dao
interface TemplateDao {

    @Upsert
    suspend fun upsert(template: MessageTemplateEntity)

    @Query("SELECT * FROM message_templates")
    fun observeAll(): Flow<List<MessageTemplateEntity>>

    @Query("SELECT * FROM message_templates WHERE templateKey = :key AND language = :language")
    suspend fun get(key: TemplateKey, language: Language): MessageTemplateEntity?

    @Query("SELECT COUNT(*) FROM message_templates")
    suspend fun count(): Int
}

@Dao
interface MessageLogDao {

    @Insert
    suspend fun insert(log: MessageLogEntity)

    @Query("SELECT * FROM message_log WHERE patientId = :patientId ORDER BY sentAt DESC")
    fun observeForPatient(patientId: String): Flow<List<MessageLogEntity>>

    @Query(
        """
        SELECT * FROM message_log
        WHERE patientId = :patientId AND templateKey = :key
        ORDER BY sentAt DESC LIMIT 1
        """
    )
    suspend fun latest(patientId: String, key: TemplateKey): MessageLogEntity?

    @Query("SELECT COUNT(*) FROM message_log WHERE sentAt >= :since")
    fun observeSentSince(since: Long): Flow<Int>
}
