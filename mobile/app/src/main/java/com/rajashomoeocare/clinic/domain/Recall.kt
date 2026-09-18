package com.rajashomoeocare.clinic.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Spec §3 groups the due list as this week / this month / over a month. */
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

private val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

fun LocalDate.displayDate(): String = format(DISPLAY_DATE)

fun LocalDate.shortDate(): String = format(SHORT_DATE)
