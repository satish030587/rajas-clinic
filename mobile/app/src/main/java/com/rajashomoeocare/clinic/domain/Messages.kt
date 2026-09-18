package com.rajashomoeocare.clinic.domain

import com.rajashomoeocare.clinic.data.local.Language
import com.rajashomoeocare.clinic.data.local.MessageTemplateEntity
import com.rajashomoeocare.clinic.data.local.TemplateKey
import java.time.LocalDate

const val CLINIC_NAME = "Rajas Homoeo Care"

/**
 * Placeholders the doctor can use when editing templates. Kept short and
 * self-explanatory because he edits these in the app, not in code.
 */
val TEMPLATE_PLACEHOLDERS = listOf("{name}", "{clinic}", "{due_date}", "{last_visit}")

fun renderTemplate(
    body: String,
    patientName: String,
    dueDate: LocalDate? = null,
    lastVisit: LocalDate? = null,
): String = body
    .replace("{name}", patientName)
    .replace("{clinic}", CLINIC_NAME)
    .replace("{due_date}", dueDate?.displayDate().orEmpty())
    .replace("{last_visit}", lastVisit?.displayDate().orEmpty())

/**
 * Seed wording. The doctor is expected to tune these after a few weeks of use
 * (spec §3), which is why they live in the database rather than in strings.xml.
 */
fun defaultTemplates(): List<MessageTemplateEntity> = listOf(
    MessageTemplateEntity(
        templateKey = TemplateKey.WELCOME,
        language = Language.EN,
        bodyText = "Dear {name}, welcome to {clinic}. " +
            "Thank you for visiting us today. Save this number for any questions " +
            "about your treatment, and we will remind you when your next visit is due.\n\n" +
            "— Dr M Ilayaraja, {clinic}",
    ),
    MessageTemplateEntity(
        templateKey = TemplateKey.WELCOME,
        language = Language.TA,
        bodyText = "அன்புள்ள {name}, {clinic} நிலையத்திற்கு வரவேற்கிறோம். " +
            "இன்று எங்களை நாடியதற்கு நன்றி. சிகிச்சை குறித்த சந்தேகங்களுக்கு இந்த எண்ணைச் " +
            "சேமித்து வைக்கவும். அடுத்த வருகை நேரத்தில் நாங்கள் நினைவூட்டுவோம்.\n\n" +
            "— டாக்டர் M இளையராஜா, {clinic}",
    ),
    MessageTemplateEntity(
        templateKey = TemplateKey.RECALL,
        language = Language.EN,
        bodyText = "Dear {name}, this is a reminder from {clinic}. " +
            "Your next visit was due on {due_date}. " +
            "Please come in soon so we can continue your treatment without a gap.\n\n" +
            "— Dr M Ilayaraja, {clinic}",
    ),
    MessageTemplateEntity(
        templateKey = TemplateKey.RECALL,
        language = Language.TA,
        bodyText = "அன்புள்ள {name}, {clinic} நிலையத்திலிருந்து நினைவூட்டல். " +
            "உங்கள் அடுத்த வருகை {due_date} அன்று வர வேண்டியிருந்தது. " +
            "சிகிச்சை தொடர்ச்சியாக இருக்க விரைவில் வருகை தரவும்.\n\n" +
            "— டாக்டர் M இளையராஜா, {clinic}",
    ),
)
