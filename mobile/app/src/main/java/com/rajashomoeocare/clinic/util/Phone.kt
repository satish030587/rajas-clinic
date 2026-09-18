package com.rajashomoeocare.clinic.util

private val INDIAN_MOBILE = Regex("^[6-9]\\d{9}$")

/**
 * Normalises to the bare 10-digit number the backend also stores
 * (see app/schemas/patient.py), so the two stay comparable when sync lands.
 */
fun normalizePhone(raw: String): String? {
    var digits = raw.filter(Char::isDigit)
    if (digits.length == 12 && digits.startsWith("91")) digits = digits.drop(2)
    if (digits.length == 11 && digits.startsWith("0")) digits = digits.drop(1)
    return if (INDIAN_MOBILE.matches(digits)) digits else null
}

fun formatPhone(phone: String): String =
    if (phone.length == 10) "${phone.take(5)} ${phone.drop(5)}" else phone
