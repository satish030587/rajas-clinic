package com.rajashomoeocare.clinic.domain

import com.rajashomoeocare.clinic.data.local.PatientRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * How overdue a recall is. Spec §3 groups the due list as
 * "this week / this month / over a month".
 */
enum class RecallBucket { DUE_TODAY, THIS_WEEK, THIS_MONTH, OVER_A_MONTH }

data class RecallItem(
    val patient: PatientRow,
    val dueDate: LocalDate,
    val daysOverdue: Int,
) {
    val bucket: RecallBucket
        get() = when {
            daysOverdue <= 0 -> RecallBucket.DUE_TODAY
            daysOverdue <= 7 -> RecallBucket.THIS_WEEK
            daysOverdue <= 30 -> RecallBucket.THIS_MONTH
            else -> RecallBucket.OVER_A_MONTH
        }
}

fun List<PatientRow>.toRecallItems(today: LocalDate): List<RecallItem> =
    mapNotNull { row ->
        val due = row.nextVisitDue ?: return@mapNotNull null
        RecallItem(
            patient = row,
            dueDate = due,
            daysOverdue = ChronoUnit.DAYS.between(due, today).toInt(),
        )
    }

/** Age from a date of birth, or from an age captured on a past date. */
fun currentAge(
    dateOfBirth: LocalDate?,
    ageYears: Int?,
    ageRecordedOn: LocalDate?,
    today: LocalDate = LocalDate.now(),
): Int? = when {
    dateOfBirth != null -> ChronoUnit.YEARS.between(dateOfBirth, today).toInt()
    ageYears != null && ageRecordedOn != null ->
        ageYears + ChronoUnit.YEARS.between(ageRecordedOn, today).toInt()
    ageYears != null -> ageYears
    else -> null
}

val PatientRow.age: Int?
    get() = currentAge(dateOfBirth, ageYears, ageRecordedOn)

private val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

fun LocalDate.displayDate(): String = format(DISPLAY_DATE)

fun LocalDate.shortDate(): String = format(SHORT_DATE)
