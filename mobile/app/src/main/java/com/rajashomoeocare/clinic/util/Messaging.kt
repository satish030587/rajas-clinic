package com.rajashomoeocare.clinic.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/**
 * Spec §3/§6: v1 sends recall messages through wa.me click-to-send links.
 * No WhatsApp Business API, no per-message cost — the app pre-fills, the doctor taps send.
 */
fun Context.openWhatsApp(phone: String, message: String): Boolean {
    val number = "91${phone.takeLast(10)}"
    val uri = "https://wa.me/$number?text=${Uri.encode(message)}".toUri()
    return try {
        startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

fun Context.dialNumber(phone: String): Boolean = try {
    startActivity(
        Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    true
} catch (_: ActivityNotFoundException) {
    false
}
