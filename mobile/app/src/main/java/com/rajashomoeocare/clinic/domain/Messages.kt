package com.rajashomoeocare.clinic.domain

import com.rajashomoeocare.clinic.data.remote.ClinicProfileDto
import java.time.LocalDate

/**
 * Placeholders the doctor can use when editing templates (spec §4.6). Kept
 * short and self-explanatory because he edits these in the app, not in code.
 */
val TEMPLATE_PLACEHOLDERS = listOf(
    "{name}", "{clinic}", "{due_date}", "{last_visit}", "{appointment_date}",
    "{working_hours}", "{clinic_address}", "{map_link}", "{clinic_phone}",
    "{tracking_id}", "{review_link}",
)

/**
 * Fills a template. Clinic details come from the server-held profile so no
 * address, phone or link is ever hard-coded into the app.
 */
fun renderTemplate(
    body: String,
    patientName: String,
    clinic: ClinicProfileDto?,
    dueDate: LocalDate? = null,
    lastVisit: LocalDate? = null,
    appointmentDate: LocalDate? = null,
    trackingId: String? = null,
): String {
    val address = listOfNotNull(
        clinic?.address?.takeIf { it.isNotBlank() },
        clinic?.landmark?.takeIf { it.isNotBlank() },
    ).joinToString("\n")

    return body
        .replace("{name}", patientName)
        .replace("{clinic}", clinic?.name.orEmpty())
        .replace("{due_date}", dueDate?.displayDate().orEmpty())
        .replace("{last_visit}", lastVisit?.displayDate().orEmpty())
        .replace("{appointment_date}", appointmentDate?.displayDate().orEmpty())
        .replace("{working_hours}", clinic?.workingHours.orEmpty())
        .replace("{clinic_address}", address)
        .replace("{map_link}", clinic?.mapLink.orEmpty())
        .replace("{clinic_phone}", clinic?.phone.orEmpty())
        .replace("{tracking_id}", trackingId.orEmpty())
        .replace("{review_link}", clinic?.reviewLink.orEmpty())
}
